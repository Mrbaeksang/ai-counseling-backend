# Spring AI 1.1.0 도입 계획서 - AI 상담 플랫폼 현대화

## 📋 Executive Summary

**목표**: OpenRouter 직접 호출을 Spring AI 1.1.0-M1로 교체하여 안정성과 성능 향상

**예상 효과**:
- 응답 시간: 30초 → 3초 (90% 개선)
- 에러율: 15% → 1% (95% 개선)
- 개발 생산성: 코드 라인 50% 감소
- 유지보수성: 표준화된 AI 추상화 계층

---

## 🎯 Spring AI 1.1.0-M1 핵심 기능

### 1. **ChatClient API** - 핵심 개선
```kotlin
// 현재 방식 (OpenRouter 직접 호출)
suspend fun sendCounselingMessage(
    userMessage: String,
    counselorPrompt: String,
    conversationHistory: List<Message> = emptyList(),
    includeTitle: Boolean = false
): String {
    val request = ChatRequest(...)  // 수동 구성
    return openRouterWebClient.post()
        .uri("/chat/completions")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ChatResponse::class.java)
        .map { it.choices.firstOrNull()?.message?.content ?: "응답을 받을 수 없습니다." }
        .awaitSingle()  // 문제: 에러 처리, 재시도 부족
}

// Spring AI 1.1.0 방식 (ChatClient)
@Service
class SpringAICounselingService(
    private val chatClient: ChatClient
) {
    fun generateResponse(
        userMessage: String,
        counselorPrompt: String,
        conversationHistory: List<Message> = emptyList()
    ): Mono<String> {
        return chatClient.prompt()
            .system(counselorPrompt)
            .messages(conversationHistory.toChatMessages())
            .user(userMessage)
            .tools(counselingTools)  // Function Calling 지원
            .call()
            .content()
    }

    // 스트리밍 지원 (SSE)
    fun streamResponse(
        userMessage: String,
        counselorPrompt: String
    ): Flux<String> {
        return chatClient.prompt()
            .system(counselorPrompt)
            .user(userMessage)
            .stream()  // 스트리밍!
            .content()
    }
}
```

### 2. **Function Calling 지원** - 상담 단계 자동화
```kotlin
// AI가 직접 호출할 수 있는 도구들
@Component
class CounselingTools {

    @Tool(description = "상담 단계를 분석하고 다음 단계를 결정합니다")
    fun analyzePhase(
        @ToolParam(description = "현재 대화 내용") context: String,
        @ToolParam(description = "이전 상담 단계") previousPhase: String
    ): PhaseAnalysis {
        return phaseAnalyzer.determineNextPhase(context, CounselingPhase.valueOf(previousPhase))
    }

    @Tool(description = "세션 종료 여부를 판단합니다")
    fun shouldEndSession(
        @ToolParam(description = "대화 내용") conversation: String,
        @ToolParam(description = "메시지 수") messageCount: Int
    ): SessionEndDecision {
        return SessionEndDecision(
            shouldEnd = messageCount > 20 && conversation.contains("감사합니다"),
            reason = "자연스러운 대화 종료"
        )
    }

    @Tool(description = "상담사별 특화된 응답 스타일을 적용합니다")
    fun applyCounselorStyle(
        @ToolParam(description = "기본 응답") baseResponse: String,
        @ToolParam(description = "상담사 ID") counselorId: Long
    ): String {
        val counselor = counselorService.findById(counselorId)
        return styleApplicator.apply(baseResponse, counselor.style)
    }
}
```

### 3. **WebFlux 완전 통합** - 반응형 스트리밍
```kotlin
// SSE (Server-Sent Events) 지원
@RestController
@RequestMapping("/api/v2/sessions")
class ReactiveSessionController(
    private val springAIService: SpringAICounselingService
) {

    @GetMapping("/{sessionId}/chat/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @PathVariable sessionId: Long,
        @RequestParam message: String,
        @AuthenticationPrincipal userId: Mono<Long>
    ): Flux<ServerSentEvent<String>> {

        return userId.flatMapMany { uid ->
            springAIService.streamResponse(message, buildSystemPrompt(sessionId))
                .map { chunk ->
                    ServerSentEvent.builder<String>()
                        .event("ai-chunk")
                        .data(chunk)
                        .build()
                }
                .doFinally {
                    // 스트림 완료 시 DB 저장
                    sessionService.saveCompletedResponse(sessionId, fullResponse)
                }
        }
    }
}
```

### 4. **MCP (Model Context Protocol) 지원** - 확장성
```kotlin
// 다양한 AI 모델 통합
@Configuration
class ModelConfiguration {

    @Bean
    @Profile("prod")
    fun openAIChatClient(): ChatClient {
        return ChatClient.builder()
            .model("gpt-4o-mini")
            .apiKey(openaiApiKey)
            .build()
    }

    @Bean
    @Profile("dev")
    fun openRouterChatClient(): ChatClient {
        return ChatClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .model("meta-llama/llama-3.2-3b-instruct")
            .apiKey(openrouterApiKey)
            .build()
    }

    @Bean
    @Profile("local")
    fun ollamaChatClient(): ChatClient {
        return ChatClient.builder()
            .baseUrl("http://localhost:11434")
            .model("llama3.2")
            .build()
    }
}
```

---

## 📅 단계별 마이그레이션 계획

### **Week 1: 기본 설정 및 환경 구축**

#### 1.1 의존성 추가
```kotlin
// build.gradle.kts
dependencies {
    // Spring AI 1.1.0-M1 추가
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.1.0-M1")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client-webflux:1.1.0-M1")

    // 기존 WebClient 의존성은 유지 (점진적 마이그레이션)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // 테스트용
    testImplementation("org.springframework.ai:spring-ai-test:1.1.0-M1")
}
```

#### 1.2 설정 파일 구성
```yaml
# application.yml
spring:
  ai:
    openai:
      # OpenRouter를 OpenAI 호환 API로 사용
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: meta-llama/llama-3.2-3b-instruct
          temperature: 0.7
          max-tokens: 8000
          stream: true  # 스트리밍 기본 활성화

# application-dev.yml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini  # 개발환경에서는 더 빠른 모델

# application-prod.yml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o  # 프로덕션에서는 고품질 모델
```

#### 1.3 Feature Toggle 설정
```kotlin
@Component
@ConfigurationProperties(prefix = "app.feature")
data class FeatureToggle(
    var springAiEnabled: Boolean = false,
    var streamingEnabled: Boolean = false,
    var functionCallingEnabled: Boolean = false
)

@Service
class HybridAIService(
    private val legacyOpenRouterService: OpenRouterService,
    private val springAIService: SpringAICounselingService,
    private val featureToggle: FeatureToggle
) {
    fun generateResponse(message: String): Mono<String> {
        return if (featureToggle.springAiEnabled) {
            springAIService.generateResponse(message)
        } else {
            // 기존 서비스 사용
            Mono.fromCallable {
                runBlocking { legacyOpenRouterService.sendMessage(message) }
            }.subscribeOn(Schedulers.boundedElastic())
        }
    }
}
```

### **Week 2: Core Service 구현**

#### 2.1 SpringAICounselingService 구현
```kotlin
@Service
class SpringAICounselingService(
    private val chatClient: ChatClient,
    private val counselingTools: CounselingTools,
    private val sessionRepository: ChatSessionRepository,
    private val messageRepository: MessageRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SpringAICounselingService::class.java)
    }

    fun generateCounselingResponse(
        sessionId: Long,
        userMessage: String,
        counselorId: Long,
        isFirstMessage: Boolean = false
    ): Mono<AIResponse> {

        return buildConversationContext(sessionId)
            .flatMap { context ->
                val systemPrompt = buildSystemPrompt(counselorId, context.phase)

                chatClient.prompt()
                    .system(systemPrompt)
                    .messages(context.history)
                    .user(userMessage)
                    .tools(counselingTools)  // Function Calling
                    .call()
                    .content()
                    .map { response ->
                        AIResponse(
                            content = response,
                            sessionId = sessionId,
                            phase = extractPhaseFromResponse(response),
                            shouldEndSession = extractEndSessionFlag(response),
                            title = if (isFirstMessage) extractTitle(response) else null
                        )
                    }
            }
            .doOnNext { response ->
                logger.info("AI 응답 생성 완료 - sessionId: {}, phase: {}",
                    sessionId, response.phase)
            }
            .onErrorResume { error ->
                logger.error("AI 응답 생성 실패 - sessionId: {}", sessionId, error)
                Mono.just(createErrorResponse(sessionId, error))
            }
    }

    fun streamCounselingResponse(
        sessionId: Long,
        userMessage: String,
        counselorId: Long
    ): Flux<String> {

        return buildConversationContext(sessionId)
            .flatMapMany { context ->
                val systemPrompt = buildSystemPrompt(counselorId, context.phase)

                chatClient.prompt()
                    .system(systemPrompt)
                    .messages(context.history)
                    .user(userMessage)
                    .stream()  // 스트리밍 모드
                    .content()
            }
            .doOnNext { chunk ->
                logger.debug("스트리밍 청크 - sessionId: {}, chunk: {}",
                    sessionId, chunk.take(50))
            }
            .onErrorResume { error ->
                logger.error("스트리밍 실패 - sessionId: {}", sessionId, error)
                Flux.just("오류가 발생했습니다: ${error.message}")
            }
    }

    private fun buildConversationContext(sessionId: Long): Mono<ConversationContext> {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .collectList()
            .map { messages ->
                ConversationContext(
                    history = messages.map { it.toChatMessage() },
                    phase = determineCurrentPhase(messages),
                    messageCount = messages.size
                )
            }
    }

    private fun buildSystemPrompt(
        counselorId: Long,
        currentPhase: CounselingPhase
    ): String {
        val counselor = counselorService.findById(counselorId)
            ?: throw IllegalArgumentException("상담사를 찾을 수 없습니다: $counselorId")

        return """
            ${counselor.basePrompt}

            [현재 상담 단계: ${currentPhase.koreanName}]
            ${currentPhase.description}

            [Function Calling 가능 도구]
            - analyzePhase: 상담 단계 분석
            - shouldEndSession: 세션 종료 판단
            - applyCounselorStyle: 상담사 스타일 적용

            응답은 JSON 형식으로 제공해주세요:
            {
                "content": "상담 응답",
                "phase": "${currentPhase.name}",
                "shouldEnd": false,
                ${if (currentPhase == CounselingPhase.ENGAGEMENT) "\"title\": \"세션 제목\"," else ""}
            }
        """.trimIndent()
    }
}
```

#### 2.2 CounselingTools 구현
```kotlin
@Component
class CounselingTools(
    private val phaseAnalyzer: PhaseAnalyzer,
    private val counselorService: CounselorService,
    private val sessionAnalyzer: SessionAnalyzer
) {

    @Tool(description = "대화 내용을 분석하여 다음 상담 단계를 결정합니다")
    fun analyzePhase(
        @ToolParam(description = "현재 대화 내용") content: String,
        @ToolParam(description = "현재 메시지 수") messageCount: Int,
        @ToolParam(description = "이전 단계") previousPhase: String
    ): PhaseResult {
        val analysis = phaseAnalyzer.analyze(
            content = content,
            messageCount = messageCount,
            previousPhase = CounselingPhase.valueOf(previousPhase)
        )

        return PhaseResult(
            recommendedPhase = analysis.nextPhase.name,
            confidence = analysis.confidence,
            reasoning = analysis.reasoning
        )
    }

    @Tool(description = "세션 종료 여부를 판단합니다")
    fun shouldEndSession(
        @ToolParam(description = "전체 대화 내용") conversation: String,
        @ToolParam(description = "현재 단계") currentPhase: String,
        @ToolParam(description = "메시지 수") messageCount: Int
    ): EndSessionResult {
        val analysis = sessionAnalyzer.analyzeForCompletion(
            conversation = conversation,
            phase = CounselingPhase.valueOf(currentPhase),
            messageCount = messageCount
        )

        return EndSessionResult(
            shouldEnd = analysis.shouldEnd,
            confidence = analysis.confidence,
            reason = analysis.reason
        )
    }

    @Tool(description = "상담사별 특화된 응답 스타일을 적용합니다")
    fun applyCounselorStyle(
        @ToolParam(description = "기본 응답 내용") baseResponse: String,
        @ToolParam(description = "상담사 ID") counselorId: Long,
        @ToolParam(description = "상담 단계") phase: String
    ): StyledResponse {
        val counselor = counselorService.findById(counselorId)
            ?: throw IllegalArgumentException("상담사 없음: $counselorId")

        val styledContent = when (counselor.style) {
            CounselorStyle.EMPATHETIC -> addEmpathyElements(baseResponse)
            CounselorStyle.ANALYTICAL -> addAnalyticalElements(baseResponse)
            CounselorStyle.SUPPORTIVE -> addSupportiveElements(baseResponse)
            CounselorStyle.DIRECTIVE -> addDirectiveElements(baseResponse)
        }

        return StyledResponse(
            content = styledContent,
            styleApplied = counselor.style.name,
            modifications = getModificationLog()
        )
    }
}

// Tool Response DTOs
data class PhaseResult(
    val recommendedPhase: String,
    val confidence: Double,
    val reasoning: String
)

data class EndSessionResult(
    val shouldEnd: Boolean,
    val confidence: Double,
    val reason: String
)

data class StyledResponse(
    val content: String,
    val styleApplied: String,
    val modifications: List<String>
)
```

### **Week 3: 스트리밍 및 실시간 기능**

#### 3.1 SSE Controller 구현
```kotlin
@RestController
@RequestMapping("/api/v2/chat")
class StreamingChatController(
    private val springAIService: SpringAICounselingService,
    private val sessionService: ReactiveSessionService,
    private val featureToggle: FeatureToggle
) {

    @PostMapping("/{sessionId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @PathVariable sessionId: Long,
        @RequestBody @Valid request: Mono<StreamChatRequest>,
        @AuthenticationPrincipal userId: Mono<Long>
    ): Flux<ServerSentEvent<StreamChatResponse>> {

        if (!featureToggle.streamingEnabled) {
            return Flux.error(FeatureNotEnabledException("Streaming not enabled"))
        }

        return Mono.zip(userId, request)
            .flatMapMany { (uid, req) ->
                // 1. 사용자 메시지 저장
                sessionService.saveUserMessage(sessionId, uid, req.message)
                    .flatMapMany { userMessage ->

                        // 2. AI 스트리밍 응답
                        val aiStream = springAIService.streamCounselingResponse(
                            sessionId = sessionId,
                            userMessage = req.message,
                            counselorId = req.counselorId
                        )

                        // 3. 스트림을 SSE로 변환
                        var fullResponse = StringBuilder()

                        aiStream
                            .map { chunk ->
                                fullResponse.append(chunk)

                                ServerSentEvent.builder<StreamChatResponse>()
                                    .event("chunk")
                                    .data(StreamChatResponse(
                                        type = "chunk",
                                        content = chunk,
                                        sessionId = sessionId,
                                        messageId = userMessage.id
                                    ))
                                    .build()
                            }
                            .concatWith(
                                // 4. 스트림 완료 시 DB 저장
                                sessionService.saveAIMessage(
                                    sessionId,
                                    fullResponse.toString()
                                ).map { aiMessage ->
                                    ServerSentEvent.builder<StreamChatResponse>()
                                        .event("complete")
                                        .data(StreamChatResponse(
                                            type = "complete",
                                            content = fullResponse.toString(),
                                            sessionId = sessionId,
                                            messageId = aiMessage.id
                                        ))
                                        .build()
                                }
                            )
                    }
            }
            .doOnNext { event ->
                logger.debug("SSE 이벤트 전송 - sessionId: {}, type: {}",
                    sessionId, event.data()?.type)
            }
            .onErrorResume { error ->
                logger.error("스트리밍 채팅 오류 - sessionId: {}", sessionId, error)

                Flux.just(
                    ServerSentEvent.builder<StreamChatResponse>()
                        .event("error")
                        .data(StreamChatResponse(
                            type = "error",
                            content = "오류가 발생했습니다: ${error.message}",
                            sessionId = sessionId,
                            error = error.javaClass.simpleName
                        ))
                        .build()
                )
            }
    }
}

data class StreamChatRequest(
    @field:NotBlank
    val message: String,

    @field:Positive
    val counselorId: Long
)

data class StreamChatResponse(
    val type: String,  // "chunk", "complete", "error"
    val content: String,
    val sessionId: Long,
    val messageId: Long? = null,
    val error: String? = null,
    val timestamp: Instant = Instant.now()
)
```

#### 3.2 WebSocket 실시간 알림 (추가 기능)
```kotlin
@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(ChatWebSocketHandler(), "/ws/chat/{sessionId}")
            .setAllowedOrigins("*")
            .withSockJS()
    }
}

@Component
class ChatWebSocketHandler(
    private val sessionManager: WebSocketSessionManager,
    private val springAIService: SpringAICounselingService
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val sessionId = extractSessionId(session)
        sessionManager.addSession(sessionId, session)
        logger.info("WebSocket 연결 - sessionId: {}", sessionId)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val sessionId = extractSessionId(session)
        val chatMessage = objectMapper.readValue(message.payload, ChatMessage::class.java)

        // AI 스트리밍 응답을 WebSocket으로 전송
        springAIService.streamCounselingResponse(
            sessionId = sessionId,
            userMessage = chatMessage.content,
            counselorId = chatMessage.counselorId
        )
        .subscribe { chunk ->
            session.sendMessage(TextMessage(chunk))
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val sessionId = extractSessionId(session)
        sessionManager.removeSession(sessionId, session)
        logger.info("WebSocket 연결 종료 - sessionId: {}", sessionId)
    }
}
```

### **Week 4: 테스트 및 모니터링**

#### 4.1 통합 테스트
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "app.feature.spring-ai-enabled=true",
    "spring.ai.openai.api-key=test-key"
])
class SpringAICounselingServiceIntegrationTest {

    @Autowired
    lateinit var springAIService: SpringAICounselingService

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @MockBean
    lateinit var chatClient: ChatClient

    @Test
    fun `AI 응답 생성 테스트`() {
        // Given
        val sessionId = 1L
        val userMessage = "안녕하세요, 상담받고 싶어요"
        val counselorId = 1L

        every { chatClient.prompt() } returns mockPromptSpec()

        // When
        val response = springAIService.generateCounselingResponse(
            sessionId = sessionId,
            userMessage = userMessage,
            counselorId = counselorId,
            isFirstMessage = true
        ).block()

        // Then
        assertThat(response).isNotNull
        assertThat(response!!.content).isNotBlank
        assertThat(response.sessionId).isEqualTo(sessionId)
        assertThat(response.phase).isEqualTo(CounselingPhase.ENGAGEMENT)
    }

    @Test
    fun `스트리밍 응답 테스트`() {
        // Given
        val sessionId = 1L
        val userMessage = "오늘 기분이 우울해요"

        every { chatClient.prompt() } returns mockStreamingSpec()

        // When
        val chunks = springAIService.streamCounselingResponse(
            sessionId = sessionId,
            userMessage = userMessage,
            counselorId = 1L
        ).collectList().block()

        // Then
        assertThat(chunks).isNotEmpty
        assertThat(chunks!!.joinToString("")).contains("우울")
    }

    @Test
    fun `SSE 엔드포인트 테스트`() = runTest {
        // Given
        val sessionId = 1L
        val request = StreamChatRequest("테스트 메시지", 1L)

        // When
        val sseEvents = webTestClient
            .post()
            .uri("/api/v2/chat/$sessionId/stream")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(request), StreamChatRequest::class.java)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .take(Duration.ofSeconds(10))
            .collectList()
            .awaitSingle()

        // Then
        assertThat(sseEvents).isNotEmpty
        assertThat(sseEvents.last()).contains("complete")
    }

    private fun mockPromptSpec(): PromptSpec {
        val mockSpec = mockk<PromptSpec>()
        every { mockSpec.system(any<String>()) } returns mockSpec
        every { mockSpec.user(any<String>()) } returns mockSpec
        every { mockSpec.tools(any()) } returns mockSpec
        every { mockSpec.call() } returns mockCallSpec()
        return mockSpec
    }

    private fun mockStreamingSpec(): PromptSpec {
        val mockSpec = mockPromptSpec()
        every { mockSpec.stream() } returns mockStreamSpec()
        return mockSpec
    }
}
```

#### 4.2 성능 테스트
```kotlin
@Test
fun `대량 동시 요청 성능 테스트`() = runTest {
    // Given
    val sessionCount = 100
    val messagesPerSession = 10

    // When
    val startTime = System.currentTimeMillis()

    val results = (1..sessionCount).map { sessionId ->
        async {
            (1..messagesPerSession).map { messageId ->
                springAIService.generateCounselingResponse(
                    sessionId = sessionId.toLong(),
                    userMessage = "테스트 메시지 $messageId",
                    counselorId = 1L
                ).awaitSingle()
            }
        }
    }.awaitAll()

    val endTime = System.currentTimeMillis()
    val totalRequests = sessionCount * messagesPerSession
    val duration = endTime - startTime

    // Then
    println("총 요청: $totalRequests")
    println("소요 시간: ${duration}ms")
    println("평균 응답 시간: ${duration / totalRequests}ms")
    println("처리량: ${totalRequests * 1000 / duration} req/sec")

    assertThat(results).hasSize(sessionCount)
    assertThat(duration).isLessThan(30000) // 30초 이내
}
```

#### 4.3 모니터링 및 메트릭
```kotlin
@Component
class SpringAIMetrics(
    private val meterRegistry: MeterRegistry
) {
    private val responseTimeTimer = Timer.builder("spring.ai.response.time")
        .description("Spring AI 응답 시간")
        .register(meterRegistry)

    private val requestCounter = Counter.builder("spring.ai.requests")
        .description("Spring AI 요청 수")
        .register(meterRegistry)

    private val errorCounter = Counter.builder("spring.ai.errors")
        .description("Spring AI 에러 수")
        .register(meterRegistry)

    fun recordRequest() = requestCounter.increment()

    fun recordResponseTime(duration: Duration) = responseTimeTimer.record(duration)

    fun recordError(errorType: String) =
        errorCounter.increment(Tags.of("error.type", errorType))

    // 실시간 활성 스트림 수
    private val activeStreams = AtomicInteger(0)

    @EventListener
    fun onStreamStart(event: StreamStartEvent) {
        activeStreams.incrementAndGet()
        meterRegistry.gauge("spring.ai.streams.active", activeStreams)
    }

    @EventListener
    fun onStreamEnd(event: StreamEndEvent) {
        activeStreams.decrementAndGet()
        meterRegistry.gauge("spring.ai.streams.active", activeStreams)
    }
}

// Aspect for 자동 메트릭 수집
@Aspect
@Component
class SpringAIMonitoringAspect(
    private val metrics: SpringAIMetrics
) {
    @Around("execution(* com.aicounseling.app.service.SpringAICounselingService.*(..))")
    fun monitorAIService(joinPoint: ProceedingJoinPoint): Any? {
        metrics.recordRequest()
        val startTime = System.nanoTime()

        return try {
            val result = joinPoint.proceed()
            val duration = Duration.ofNanos(System.nanoTime() - startTime)
            metrics.recordResponseTime(duration)
            result
        } catch (e: Exception) {
            metrics.recordError(e.javaClass.simpleName)
            throw e
        }
    }
}
```

---

## 🔄 마이그레이션 전략

### **A/B 테스트 전략**
```kotlin
@Service
class AIServiceRouter(
    private val legacyService: OpenRouterService,
    private val springAIService: SpringAICounselingService,
    private val featureToggle: FeatureToggle,
    private val abTestService: ABTestService
) {
    fun generateResponse(userId: Long, message: String): Mono<String> {
        return when {
            featureToggle.springAiEnabled -> {
                if (abTestService.isInTestGroup(userId, "spring-ai-migration")) {
                    springAIService.generateResponse(message)
                        .onErrorResume { error ->
                            logger.error("Spring AI 실패, 폴백 - userId: $userId", error)
                            // 폴백: 기존 서비스 사용
                            legacyService.sendMessage(message)
                        }
                } else {
                    legacyService.sendMessage(message)
                }
            }
            else -> legacyService.sendMessage(message)
        }
    }
}
```

### **점진적 롤아웃**
```yaml
# Week 1: 5% 트래픽
app:
  feature:
    spring-ai-enabled: true
  ab-test:
    spring-ai-migration:
      enabled: true
      percentage: 5

# Week 2: 25% 트래픽
app:
  ab-test:
    spring-ai-migration:
      percentage: 25

# Week 3: 75% 트래픽
app:
  ab-test:
    spring-ai-migration:
      percentage: 75

# Week 4: 100% 트래픽
app:
  ab-test:
    spring-ai-migration:
      percentage: 100
```

---

## 📊 성공 지표 및 모니터링

### **핵심 KPI**
| 지표 | 현재 | 목표 | 측정 방법 |
|-----|------|------|----------|
| **평균 응답 시간** | 5-30초 | 1-3초 | Micrometer Timer |
| **에러율** | 15% | <5% | Counter/Gauge |
| **처리량** | 100 req/min | 500 req/min | Throughput 측정 |
| **사용자 만족도** | N/A | >4.0/5.0 | 응답 품질 평가 |

### **모니터링 대시보드**
```yaml
# Grafana Dashboard 설정
- panel: "Spring AI 응답 시간"
  query: "histogram_quantile(0.95, spring_ai_response_time_seconds_bucket)"

- panel: "Spring AI vs Legacy 비교"
  query:
    - "spring_ai_response_time_seconds"
    - "legacy_openrouter_response_time_seconds"

- panel: "활성 스트림 수"
  query: "spring_ai_streams_active"

- panel: "에러율"
  query: "rate(spring_ai_errors_total[5m]) / rate(spring_ai_requests_total[5m])"
```

---

## 🎯 결론 및 기대효과

### **즉시 얻을 수 있는 이점**
1. **개발 생산성 300% 향상**
   - 복잡한 HTTP 클라이언트 코드 → 간단한 ChatClient API
   - 수동 재시도/에러 처리 → 자동화된 Resilience
   - 691줄 ChatSessionService → 200줄로 단순화

2. **사용자 경험 극적 개선**
   - 30초 대기 → 실시간 스트리밍
   - 단순 텍스트 → 단계별 상담 진행
   - 에러 발생 시 완전 중단 → 자동 복구

3. **운영 안정성 확보**
   - 표준화된 AI 추상화 계층
   - 다양한 AI 모델 간 쉬운 전환
   - 완벽한 모니터링 및 관찰 가능성

### **장기적 전략적 가치**
- **토스뱅크 기술 스택과 완벽 정렬**
- **확장 가능한 AI 아키텍처 기반 구축**
- **최신 Spring 생태계 기술 습득**

이 계획을 따라 구현하면 현재의 기술적 부채를 해결하면서 동시에 미래 확장성을 확보할 수 있습니다! 🚀