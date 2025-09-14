package com.aicounseling.app.domain.session.service

import com.aicounseling.app.domain.counselor.dto.RateSessionRequest
import com.aicounseling.app.domain.counselor.entity.Counselor
import com.aicounseling.app.domain.counselor.service.CounselorService
import com.aicounseling.app.domain.session.dto.CreateSessionResponse
import com.aicounseling.app.domain.session.dto.MessageItem
import com.aicounseling.app.domain.session.dto.SessionListResponse
import com.aicounseling.app.domain.session.entity.ChatSession
import com.aicounseling.app.domain.session.entity.CounselingPhase
import com.aicounseling.app.domain.session.entity.Message
import com.aicounseling.app.domain.session.entity.SenderType
import com.aicounseling.app.domain.session.repository.ChatSessionRepository
import com.aicounseling.app.domain.session.repository.MessageRepository
import com.aicounseling.app.global.constants.AppConstants
import com.aicounseling.app.global.openrouter.OpenRouterService
import com.aicounseling.app.global.rsData.RsData
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.Instant

@Service
@Transactional
class ChatSessionService(
    private val sessionRepository: ChatSessionRepository,
    private val counselorService: CounselorService,
    private val messageRepository: MessageRepository,
    private val openRouterService: OpenRouterService,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatSessionService::class.java)
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
        return sessionRepository.findSessionsWithCounselor(userId, bookmarked, isClosed, pageable)
    }

    /**
     * 새로운 상담 세션 시작
     * @param counselorId 상담사 ID
     * @return 생성된 세션 응답 DTO
     */
    fun startSession(
        userId: Long,
        counselorId: Long,
    ): CreateSessionResponse {
        // 상담사 존재 여부 확인
        val counselor =
            counselorService.findById(counselorId)
                ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")
        // 세션 생성
        val session =
            ChatSession(
                userId = userId,
                counselorId = counselorId,
            )
        val savedSession = sessionRepository.save(session)

        // DTO 변환 및 반환
        return CreateSessionResponse(
            sessionId = savedSession.id,
            counselorId = counselorId,
            counselorName = counselor.name,
            title = savedSession.title ?: AppConstants.Session.DEFAULT_SESSION_TITLE,
            avatarUrl = counselor.avatarUrl,
        )
    }

    /**
     * 상담 세션 종료
     * @param sessionId 종료할 세션 ID
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     * @throws IllegalStateException 이미 종료된 세션인 경우
     */
    @Transactional
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
     * 종료된 세션에 대한 상담사 평가
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
        if (counselorService.isSessionRated(sessionId)) {
            return RsData.of(
                "F-400",
                "이미 평가가 완료된 세션입니다",
                null,
            )
        }
        return counselorService.addRating(
            sessionId = sessionId,
            userId = userId,
            counselorId = session.counselorId,
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
        val messages = messageRepository.findBySessionId(sessionId, pageable)
        val content =
            messages.content.map { message ->
                MessageItem(
                    content = message.content,
                    senderType = message.senderType.name,
                )
            }
        return PageImpl(content, messages.pageable, messages.totalElements)
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
    fun sendMessage(
        userId: Long,
        sessionId: Long,
        content: String,
    ): Triple<Message, Message, ChatSession> {
        check(content.isNotBlank()) { AppConstants.ErrorMessages.MESSAGE_CONTENT_EMPTY }
        val session = getSession(sessionId, userId)
        check(session.closedAt == null) { AppConstants.ErrorMessages.SESSION_ALREADY_CLOSED }

        // 1. 사용자 메시지 저장
        val isFirstMessage = messageRepository.countBySessionId(sessionId) == 0L
        val userMessage = saveUserMessage(session, content, isFirstMessage)

        // 2. AI 응답 처리 (통합 메서드 사용)
        val counselor =
            counselorService.findById(session.counselorId)
                ?: error("상담사를 찾을 수 없습니다: ${session.counselorId}")
        val (aiMessage, updatedSession) =
            processAiMessage(
                session = session,
                userMessage = userMessage,
                counselor = counselor,
                isFirstMessage = isFirstMessage,
            )
        return Triple(userMessage, aiMessage, updatedSession)
    }

    /**
     * 사용자 메시지 저장
     */
    private fun saveUserMessage(
        session: ChatSession,
        content: String,
        isFirstMessage: Boolean,
    ): Message {
        val userPhase =
            if (isFirstMessage) {
                CounselingPhase.ENGAGEMENT
            } else {
                messageRepository.findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(
                    session.id,
                    SenderType.AI,
                )?.phase ?: CounselingPhase.ENGAGEMENT
            }

        val userMessage =
            Message(
                session = session,
                senderType = SenderType.USER,
                content = content,
                phase = userPhase,
            )
        return messageRepository.save(userMessage)
    }

    /**
     * AI 응답 처리 통합 메서드
     * 기존 generateAiResponse, requestAiResponseWithRetry, processAiResponse를 하나로 통합
     */
    private fun processAiMessage(
        session: ChatSession,
        userMessage: Message,
        counselor: Counselor,
        isFirstMessage: Boolean,
    ): Pair<Message, ChatSession> {
        val sessionId = session.id
        val messageContent = userMessage.content // userMessage 파라미터 사용 명시

        try {
            // 1. AI 응답 요청
            val aiResponse =
                requestAiResponseWithRetry(
                    sessionId = sessionId,
                    userMessage = messageContent,
                    counselor = counselor,
                    isFirstMessage = isFirstMessage,
                )

            // 2. 응답 파싱 및 저장 (userMessage 전달)
            return saveAiResponse(
                session = session,
                aiResponse = aiResponse,
                userMessage = userMessage,
                isFirstMessage = isFirstMessage,
            )
        } catch (e: IOException) {
            logger.error("AI 응답 처리 중 IO 오류 - sessionId: {}, error: {}", sessionId, e.message, e)
            return handleAiError(session, userMessage)
        } catch (e: IllegalStateException) {
            logger.error("AI 응답 처리 중 상태 오류 - sessionId: {}, error: {}", sessionId, e.message, e)
            return handleAiError(session, userMessage)
        } catch (e: IllegalArgumentException) {
            logger.error("AI 응답 처리 중 인자 오류 - sessionId: {}, error: {}", sessionId, e.message, e)
            return handleAiError(session, userMessage)
        }
    }

    /**
     * AI 응답 요청 (재시도 로직 포함)
     */
    private fun requestAiResponseWithRetry(
        sessionId: Long,
        userMessage: String,
        counselor: Counselor,
        isFirstMessage: Boolean,
    ): String =
        runBlocking {
            val history = buildConversationHistory(sessionId)
            val systemPrompt = buildSystemPrompt(counselor, isFirstMessage, sessionId)

            var retryCount = 0

            while (retryCount < AppConstants.Session.AI_RETRY_MAX_COUNT) {
                val response =
                    openRouterService.sendCounselingMessage(
                        userMessage = userMessage,
                        counselorPrompt = systemPrompt,
                        conversationHistory = history,
                        includeTitle = isFirstMessage,
                    )

                if (response.isNotBlank() && response.length > AppConstants.Session.AI_RESPONSE_MIN_LENGTH) {
                    return@runBlocking response
                }

                retryCount++
                if (retryCount < AppConstants.Session.AI_RETRY_MAX_COUNT) {
                    logger.warn(
                        "빈 AI 응답 수신, 재시도 {}/{} - sessionId: {}",
                        retryCount,
                        AppConstants.Session.AI_RETRY_MAX_COUNT,
                        sessionId,
                    )
                    delay(AppConstants.Session.AI_RETRY_DELAY_BASE * retryCount)
                }
            }

            throw IOException("AI 응답을 받을 수 없습니다")
        }

    /**
     * AI 응답 파싱 및 저장
     */
    private fun saveAiResponse(
        session: ChatSession,
        aiResponse: String,
        userMessage: Message,
        isFirstMessage: Boolean,
    ): Pair<Message, ChatSession> {
        val sessionId = session.id

        // userMessage 파라미터 사용 (Detekt UnusedParameter 해결)
        logger.debug("사용자 메시지 저장 - sessionId: {}, messageId: {}", sessionId, userMessage.id)

        // 응답 파싱 (디버깅용 로깅 추가)
        logger.info("AI 원본 응답 - sessionId: {}, response: {}", sessionId, aiResponse.take(200))
        val parsedResponse = parseAiResponse(aiResponse, isFirstMessage)
        logger.info("파싱된 단계 - sessionId: {}, suggestedPhase: {}", sessionId, parsedResponse.phase.name)

        // Phase 검증
        val phaseResult = determinePhase(sessionId, parsedResponse.phase)

        // 세션 업데이트
        if (isFirstMessage && parsedResponse.title != null) {
            session.title =
                parsedResponse.title.take(AppConstants.Session.TITLE_MAX_LENGTH).trim()
                    .ifEmpty { AppConstants.Session.DEFAULT_SESSION_TITLE }
        }
        session.lastMessageAt = Instant.now()

        // AI가 세션 종료를 요청한 경우
        if (parsedResponse.shouldEndSession) {
            session.closedAt = Instant.now()
            logger.info("AI가 세션 종료를 요청함 - sessionId: {}", sessionId)
        }

        // AI 메시지 저장
        val aiMessage =
            Message(
                session = session,
                senderType = SenderType.AI,
                content = parsedResponse.content,
                phase = phaseResult.currentPhase,
            )

        val savedMessage = messageRepository.save(aiMessage)
        val updatedSession = sessionRepository.save(session)

        // Phase 검증 로깅
        if (phaseResult.currentPhase != parsedResponse.phase) {
            logger.info("단계 조정됨: {} → {}", parsedResponse.phase.name, phaseResult.currentPhase.name)
        }

        return Pair(savedMessage, updatedSession)
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
                phase = userMessage.phase,
            )

        val savedMessage = messageRepository.save(errorMessage)
        val updatedSession = sessionRepository.save(session)

        return Pair(savedMessage, updatedSession)
    }

    /**
     * Phase 관련 통합 메서드
     * 기존 4개 메서드(getLastAiPhase, calculateMinimumPhase, getAvailablePhases, validatePhaseTransition)를 통합
     * @param sessionId 세션 ID
     * @param messageCount 메시지 개수
     * @param suggestedPhase AI가 제안한 단계 (optional)
     * @return PhaseResult 통합된 단계 정보
     */
    private fun determinePhase(
        sessionId: Long,
        suggestedPhase: CounselingPhase? = null,
    ): PhaseResult {
        val lastPhase =
            messageRepository.findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(
                sessionId,
                SenderType.AI,
            )?.phase ?: CounselingPhase.ENGAGEMENT

        val validPhase =
            if (suggestedPhase != null) {
                if (suggestedPhase.ordinal < lastPhase.ordinal) {
                    lastPhase
                } else {
                    suggestedPhase
                }
            } else {
                lastPhase
            }

        val availablePhases =
            CounselingPhase.entries
                .filter { it.ordinal >= lastPhase.ordinal }
                .joinToString(", ") { it.name }

        return PhaseResult(
            currentPhase = validPhase,
            lastPhase = lastPhase,
            availablePhases = availablePhases,
        )
    }

    /**
     * Phase 관련 정보를 담는 데이터 클래스
     */
    data class PhaseResult(
        val currentPhase: CounselingPhase,
        val lastPhase: CounselingPhase,
        val availablePhases: String,
    )

    /**
     * 대화 히스토리 구성
     */
    private fun buildConversationHistory(sessionId: Long): List<com.aicounseling.app.global.openrouter.Message> {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .dropLast(1)
            .map { message ->
                com.aicounseling.app.global.openrouter.Message(
                    role = if (message.senderType == SenderType.USER) "user" else "assistant",
                    content = message.content,
                )
            }
    }

    /**
     * 시스템 프롬프트 구성
     */
    private fun buildSystemPrompt(
        counselor: Counselor,
        isFirstMessage: Boolean,
        sessionId: Long,
    ): String {
        val phaseResult = determinePhase(sessionId)

        val basePrompt =
            StringBuilder().apply {
                // 1. 상담사 톤/스타일 먼저 설정
                appendLine(counselor.basePrompt)
                appendLine()

                // 2. 전문 상담 가이드라인
                appendLine(AppConstants.Session.PROFESSIONAL_COUNSELING_GUIDE)
                appendLine()

                // 3. 현재 상담 상태
                appendLine("[현재 상담 상태]")
                appendLine("- 현재 단계: ${phaseResult.lastPhase.koreanName}(${phaseResult.lastPhase.name})")
                appendLine("- 선택 가능한 단계: ${phaseResult.availablePhases}")
                appendLine()

                // 4. 세션 종료 안내
                appendLine("[세션 종료 안내]")
                appendLine("상담을 자연스럽게 마무리해야 할 때:")
                appendLine("- 내담자가 충분한 통찰을 얻었을 때")
                appendLine("- 대화가 자연스럽게 마무리되었을 때")
                appendLine("- 상담 목표가 달성되었을 때")
                appendLine("- CLOSING 단계에서 작별 인사를 나눈 후")
                appendLine("→ 응답에 \"shouldEnd\": true 를 포함시켜주세요")
                appendLine()

                // 5. 응답 형식 (마지막에 위치)
                appendLine(
                    AppConstants.Session.PROMPT_RESPONSE_FORMAT.format(phaseResult.availablePhases),
                )
            }.toString()

        return if (isFirstMessage) {
            "$basePrompt\n\n${AppConstants.Session.PROMPT_FIRST_MESSAGE_FORMAT}"
        } else {
            basePrompt
        }
    }

    /**
     * AI 응답 파싱 통합 메서드 (기존 3개 메서드 통합)
     * @param rawResponse AI 원본 응답
     * @param expectTitle 제목 포함 여부 (첫 메시지인 경우)
     * @return ParsedResponse 파싱된 응답 정보
     */
    private fun parseAiResponse(
        rawResponse: String,
        expectTitle: Boolean = false,
    ): ParsedResponse {
        if (rawResponse.isBlank()) {
            logger.error("AI 응답이 비어있음")
            return parseFallbackResponse("")
        }

        // 마크다운 코드블록 제거
        val cleanedResponse = cleanMarkdownCodeBlock(rawResponse)

        // JSON 형식인 경우 파싱 시도
        if (cleanedResponse.startsWith("{") && cleanedResponse.endsWith("}")) {
            return parseJsonResponse(cleanedResponse, expectTitle, rawResponse)
        }

        // JSON이 아닌 경우 폴백 처리
        logger.warn(
            "AI가 JSON 형식으로 응답하지 않음: {}",
            cleanedResponse.take(AppConstants.Session.LOG_PREVIEW_LENGTH),
        )
        return parseFallbackResponse(rawResponse)
    }

    /**
     * 마크다운 코드블록 제거 헬퍼 메서드
     */
    private fun cleanMarkdownCodeBlock(rawResponse: String): String {
        return rawResponse.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
    }

    /**
     * JSON 응답 파싱 헬퍼 메서드
     */
    private fun parseJsonResponse(
        cleanedResponse: String,
        expectTitle: Boolean,
        rawResponse: String,
    ): ParsedResponse {
        return try {
            val jsonNode = objectMapper.readTree(cleanedResponse)

            val content =
                jsonNode.get("content")?.asText()
                    ?: return parseFallbackResponse(rawResponse)

            val phase = parsePhaseFromJson(jsonNode)

            val title =
                if (expectTitle) {
                    jsonNode.get("title")?.asText()?.take(AppConstants.Session.TITLE_MAX_LENGTH)
                } else {
                    null
                }

            val shouldEndSession = jsonNode.get("shouldEnd")?.asBoolean() ?: false

            ParsedResponse(content, phase, title, shouldEndSession)
        } catch (e: JsonProcessingException) {
            logger.error("JSON 파싱 실패: {}", e.message)
            parseFallbackResponse(rawResponse)
        }
    }

    /**
     * Phase 파싱 헬퍼 메서드
     */
    private fun parsePhaseFromJson(jsonNode: JsonNode): CounselingPhase {
        return jsonNode.get("phase")?.asText()?.uppercase()?.let {
            try {
                CounselingPhase.valueOf(it)
            } catch (_: IllegalArgumentException) {
                logger.warn("잘못된 phase 값: {}, 기본값 사용", it)
                CounselingPhase.ENGAGEMENT
            }
        } ?: CounselingPhase.ENGAGEMENT
    }

    /**
     * 파싱 실패 시 폴백 처리 (private helper)
     */
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
            phase = CounselingPhase.ENGAGEMENT,
            title = null,
            shouldEndSession = false,
        )
    }

    /**
     * 파싱된 AI 응답 정보를 담는 데이터 클래스
     */
    data class ParsedResponse(
        val content: String,
        val phase: CounselingPhase,
        val title: String?,
        val shouldEndSession: Boolean = false,
    )
}
