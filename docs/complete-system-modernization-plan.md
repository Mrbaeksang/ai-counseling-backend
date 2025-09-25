# 완전한 시스템 현대화 계획서 - AI 상담 플랫폼

## 🎯 Overview: 5가지 핵심 개선 영역

**현재 문제점**:
- `runBlocking`으로 인한 성능 병목 (치명적)
- 단순 HTTP 호출의 한계 (에러 처리 부족)
- 캐싱 전략 부재 (매번 JWT 파싱, DB 조회)
- 동기식 처리로 인한 UX 저하 (30초 대기)
- 장애 전파 및 복구 능력 부족

**통합 솔루션**:
1. **Spring AI 1.1.0** - AI 호출 현대화
2. **Redis Reactive** - 캐싱 및 세션 관리
3. **Kafka Streams** - 이벤트 드리븐 아키텍처
4. **Controller 비동기화** - 완전한 Non-blocking
5. **Resilience4j** - 장애 복구 및 안정성

---

## 📅 통합 마이그레이션 로드맵 (6주)

### **Week 1-2: Foundation (Spring AI + Redis)**

#### Spring AI 도입
```kotlin
// 1. 의존성 추가
dependencies {
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.1.0-M1")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-cache")
}

// 2. 설정
spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
  redis:
    host: localhost
    port: 6379
```

#### Redis Reactive 통합
```kotlin
@Configuration
class RedisReactiveConfig {
    @Bean
    fun reactiveRedisTemplate(
        factory: LettuceConnectionFactory
    ): ReactiveRedisTemplate<String, Any> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = GenericJackson2JsonRedisSerializer()

        return ReactiveRedisTemplate(
            factory,
            RedisSerializationContext
                .newSerializationContext<String, Any>(keySerializer)
                .value(valueSerializer)
                .build()
        )
    }
}

@Service
class CachedAIService(
    private val chatClient: ChatClient,
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {
    fun generateWithCache(sessionId: Long, message: String): Mono<String> {
        val cacheKey = "ai:response:$sessionId:${message.hashCode()}"

        return redisTemplate.opsForValue()
            .get(cacheKey)
            .cast(String::class.java)
            .switchIfEmpty(
                // 캐시 미스 시 AI 호출
                chatClient.prompt()
                    .user(message)
                    .call()
                    .content()
                    .flatMap { response ->
                        // 5분 캐싱
                        redisTemplate.opsForValue()
                            .set(cacheKey, response, Duration.ofMinutes(5))
                            .thenReturn(response)
                    }
            )
    }
}
```

### **Week 3-4: Event-Driven Architecture (Kafka)**

#### Kafka 설정 및 이벤트 정의
```kotlin
// 1. 의존성 추가
dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.projectreactor.kafka:reactor-kafka")
}

// 2. 이벤트 정의
sealed class ChatEvent {
    abstract val sessionId: Long
    abstract val userId: Long
    abstract val timestamp: Instant
}

data class UserMessageSentEvent(
    override val sessionId: Long,
    override val userId: Long,
    val messageId: Long,
    val content: String,
    val counselorId: Long,
    override val timestamp: Instant = Instant.now()
) : ChatEvent()

data class AIResponseGeneratedEvent(
    override val sessionId: Long,
    override val userId: Long,
    val responseId: Long,
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
    override val timestamp: Instant = Instant.now()
) : ChatEvent()

// 3. Reactive Kafka 설정
@Configuration
class ReactiveKafkaConfig {
    @Bean
    fun kafkaSender(): KafkaSender<String, ChatEvent> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return KafkaSender.create(SenderOptions.create(props))
    }

    @Bean
    fun kafkaReceiver(): KafkaReceiver<String, ChatEvent> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ConsumerConfig.GROUP_ID_CONFIG to "chat-processor",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest"
        )
        return KafkaReceiver.create(ReceiverOptions.create(props))
    }
}
```

#### 이벤트 기반 메시지 처리
```kotlin
@Service
class EventDrivenChatService(
    private val kafkaSender: KafkaSender<String, ChatEvent>,
    private val messageRepository: MessageRepository,
    private val sessionService: SessionService
) {
    // 1. 사용자 메시지 - 즉시 응답
    fun handleUserMessage(
        sessionId: Long,
        userId: Long,
        content: String,
        counselorId: Long
    ): Mono<MessageResponse> {
        return messageRepository.save(
            Message.createUserMessage(sessionId, content)
        ).flatMap { savedMessage ->
            // Kafka로 이벤트 발행 (비동기)
            val event = UserMessageSentEvent(
                sessionId = sessionId,
                userId = userId,
                messageId = savedMessage.id,
                content = content,
                counselorId = counselorId
            )

            kafkaSender.send(
                Mono.just(SenderRecord.create("user-messages", event.sessionId.toString(), event, null))
            ).then(
                Mono.just(MessageResponse(
                    messageId = savedMessage.id,
                    status = "processing",
                    message = "메시지를 받았습니다. AI가 응답을 준비 중입니다."
                ))
            )
        }
    }
}

// 2. AI 응답 처리 Consumer
@Component
class AIResponseProcessor(
    private val chatClient: ChatClient,
    private val messageRepository: MessageRepository,
    private val kafkaSender: KafkaSender<String, ChatEvent>,
    private val websocketService: WebSocketService
) {
    @PostConstruct
    fun startProcessing() {
        kafkaReceiver.receive()
            .filter { it.value() is UserMessageSentEvent }
            .cast(UserMessageSentEvent::class.java)
            .flatMap { record ->
                processUserMessage(record.value())
                    .doOnSuccess { record.receiverOffset().acknowledge() }
                    .onErrorResume { error ->
                        logger.error("AI 처리 실패", error)
                        handleAIError(record.value(), error)
                            .then(Mono.empty())
                    }
            }
            .subscribe()
    }

    private fun processUserMessage(event: UserMessageSentEvent): Mono<AIResponseGeneratedEvent> {
        return chatClient.prompt()
            .system(buildSystemPrompt(event.counselorId))
            .user(event.content)
            .tools(counselingTools)
            .call()
            .content()
            .flatMap { aiResponse ->
                // AI 메시지 저장
                messageRepository.save(
                    Message.createAIMessage(event.sessionId, aiResponse)
                ).map { savedMessage ->
                    AIResponseGeneratedEvent(
                        sessionId = event.sessionId,
                        userId = event.userId,
                        responseId = savedMessage.id,
                        content = aiResponse,
                        metadata = buildResponseMetadata(aiResponse)
                    )
                }
            }
            .doOnNext { responseEvent ->
                // WebSocket으로 실시간 전송
                websocketService.sendToUser(event.userId, responseEvent)

                // 응답 이벤트 발행
                kafkaSender.send(
                    Mono.just(SenderRecord.create("ai-responses", responseEvent.sessionId.toString(), responseEvent, null))
                ).subscribe()
            }
    }
}
```

### **Week 5: Controller 완전 비동기화**

#### 기존 Controller 문제점 해결
```kotlin
// ❌ 현재 - runBlocking 사용
@RestController
class ChatSessionController {
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(@PathVariable sessionId: Long, @RequestBody request: SendMessageRequest): RsData<MessageResponse> =
        runBlocking {  // 🔴 스레드 블로킹!
            val result = chatSessionService.sendMessage(sessionId, request.content)
            RsData.of("S-1", "success", result)
        }
}

// ✅ 개선 - 완전 비동기
@RestController
@RequestMapping("/api/v2/sessions")
class ReactiveSessionController(
    private val eventDrivenChatService: EventDrivenChatService,
    private val springAIService: SpringAICounselingService
) {
    // 1. 즉시 응답 + 백그라운드 처리
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: Long,
        @RequestBody request: Mono<SendMessageRequest>,
        @AuthenticationPrincipal userId: Mono<Long>
    ): Mono<ResponseEntity<RsData<MessageResponse>>> {
        return Mono.zip(userId, request)
            .flatMap { (uid, req) ->
                eventDrivenChatService.handleUserMessage(
                    sessionId = sessionId,
                    userId = uid,
                    content = req.content,
                    counselorId = req.counselorId
                )
            }
            .map { response ->
                ResponseEntity.accepted()  // 202 Accepted
                    .header("X-Processing-Id", response.messageId.toString())
                    .body(RsData.of("S-202", "메시지 접수 완료", response))
            }
            .timeout(Duration.ofSeconds(5))
            .onErrorResume { error ->
                logger.error("메시지 처리 실패", error)
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(RsData.failOf<MessageResponse>("F-500", "메시지 처리 중 오류가 발생했습니다"))
                )
            }
    }

    // 2. 실시간 스트리밍 (SSE)
    @GetMapping("/{sessionId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamMessages(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userId: Mono<Long>
    ): Flux<ServerSentEvent<ChatStreamData>> {
        return userId.flatMapMany { uid ->
            // Redis Pub/Sub로 실시간 이벤트 수신
            redisTemplate.listenTo(ChannelTopic("session:$sessionId:events"))
                .map { message ->
                    val event = objectMapper.readValue(message.body, ChatEvent::class.java)

                    ServerSentEvent.builder<ChatStreamData>()
                        .id(event.timestamp.toString())
                        .event(event.javaClass.simpleName)
                        .data(ChatStreamData.from(event))
                        .retry(Duration.ofSeconds(5))
                        .build()
                }
                .timeout(Duration.ofMinutes(30))  // 30분 타임아웃
        }
    }

    // 3. 세션 목록 조회 (캐싱 적용)
    @GetMapping
    fun getUserSessions(
        @RequestParam(required = false) bookmarked: Boolean?,
        @RequestParam(required = false) isClosed: Boolean?,
        pageable: Pageable,
        @AuthenticationPrincipal userId: Mono<Long>
    ): Mono<ResponseEntity<RsData<PagedResponse<SessionListResponse>>>> {
        return userId.flatMap { uid ->
            val cacheKey = "sessions:$uid:${bookmarked}:${isClosed}:${pageable.pageNumber}"

            redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(PagedResponse::class.java)
                .switchIfEmpty(
                    // 캐시 미스 시 DB 조회
                    sessionService.getUserSessions(uid, bookmarked, isClosed, pageable)
                        .flatMap { result ->
                            redisTemplate.opsForValue()
                                .set(cacheKey, result, Duration.ofMinutes(5))
                                .thenReturn(result)
                        }
                )
        }.map { sessions ->
            ResponseEntity.ok(RsData.of("S-1", "조회 성공", sessions))
        }
    }
}
```

### **Week 6: Resilience4j 장애 처리**

#### Circuit Breaker 및 재시도 전략
```kotlin
// 1. 의존성 추가
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-reactor")
}

// 2. 설정
resilience4j:
  circuitbreaker:
    instances:
      ai-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        permitted-number-of-calls-in-half-open-state: 3
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 10s

  retry:
    instances:
      ai-service:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2

  ratelimiter:
    instances:
      ai-service:
        limit-for-period: 100
        limit-refresh-period: 1m
        timeout-duration: 5s

// 3. Resilient AI Service
@Service
class ResilientAIService(
    private val chatClient: ChatClient,
    private val fallbackService: AIFallbackService
) {
    private val circuitBreaker = CircuitBreaker.of("ai-service")
    private val retry = Retry.of("ai-service")
    private val rateLimiter = RateLimiter.of("ai-service")

    fun generateResponseWithResilience(
        sessionId: Long,
        message: String,
        counselorId: Long
    ): Mono<String> {
        return Mono.fromCallable {
            rateLimiter.acquirePermission()
        }.filter { it }
            .switchIfEmpty(Mono.error(RateLimitExceededException("Rate limit exceeded")))
            .then(
                chatClient.prompt()
                    .system(buildSystemPrompt(counselorId))
                    .user(message)
                    .call()
                    .content()
            )
            .transformDeferred { mono ->
                circuitBreaker.executeMonoSupplier {
                    retry.executeMonoSupplier { mono }
                }
            }
            .onErrorResume { error ->
                logger.error("AI 서비스 장애 - sessionId: {}, error: {}", sessionId, error.message)

                when (error) {
                    is CallNotPermittedException -> {
                        // Circuit Breaker Open
                        fallbackService.generateFallbackResponse(
                            "AI 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요.",
                            counselorId
                        )
                    }
                    is RateLimitExceededException -> {
                        // Rate Limit
                        fallbackService.generateFallbackResponse(
                            "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                            counselorId
                        )
                    }
                    else -> {
                        // 기타 오류
                        fallbackService.generateFallbackResponse(
                            "죄송합니다. 일시적인 오류가 발생했습니다.",
                            counselorId
                        )
                    }
                }
            }
    }
}

// 4. Fallback Service
@Service
class AIFallbackService(
    private val counselorService: CounselorService,
    private val fallbackTemplates: FallbackTemplateService
) {
    fun generateFallbackResponse(
        baseMessage: String,
        counselorId: Long
    ): Mono<String> {
        return counselorService.findById(counselorId)
            .map { counselor ->
                fallbackTemplates.applyStyle(baseMessage, counselor.style)
            }
            .defaultIfEmpty(baseMessage)
    }
}

// 5. Health Check 및 Metrics
@Component
class ResilienceMetrics(
    private val meterRegistry: MeterRegistry
) {
    private val circuitBreakerStateGauge =
        Gauge.builder("circuit.breaker.state")
            .description("Circuit Breaker 상태")
            .register(meterRegistry) {
                when (circuitBreaker.state) {
                    CircuitBreaker.State.CLOSED -> 0.0
                    CircuitBreaker.State.OPEN -> 1.0
                    CircuitBreaker.State.HALF_OPEN -> 0.5
                }
            }

    @EventListener
    fun onCircuitBreakerStateTransition(event: CircuitBreakerOnStateTransitionEvent) {
        logger.warn("Circuit Breaker 상태 변경: {} -> {}",
            event.stateTransition.fromState,
            event.stateTransition.toState)

        meterRegistry.counter(
            "circuit.breaker.state.transitions",
            "from", event.stateTransition.fromState.name,
            "to", event.stateTransition.toState.name
        ).increment()
    }
}
```

---

## 🔄 통합 아키텍처 흐름

### **메시지 처리 플로우**
```
사용자 메시지 전송
    ↓
Controller (즉시 202 응답)
    ↓
Kafka Producer (비동기 이벤트 발행)
    ↓
AI Response Consumer (백그라운드)
    ↓
Spring AI + Resilience4j (장애 처리)
    ↓
Redis Pub/Sub (실시간 알림)
    ↓
WebSocket/SSE (사용자에게 전송)
```

### **캐싱 전략**
```
JWT 검증: Redis (1시간 TTL)
    ↓
세션 목록: Redis (5분 TTL)
    ↓
AI 응답: Redis (5분 TTL, 중복 질문 방지)
    ↓
상담사 정보: Redis (24시간 TTL)
```

### **장애 처리 계층**
```
Rate Limiter (분당 100회)
    ↓
Retry (3회, 지수 백오프)
    ↓
Circuit Breaker (50% 실패율 시 차단)
    ↓
Fallback Service (상담사 스타일 맞춤 대체 응답)
```

---

## 📊 최종 성과 지표

| 개선 영역 | 현재 | 목표 | 구현 방법 |
|---------|------|------|----------|
| **응답 시간** | 30초 | 0.5초 | Spring AI + 이벤트 드리븐 |
| **동시 처리** | 200명 | 10,000명 | Kafka + Redis + 비동기 |
| **가용성** | 99% | 99.9% | Resilience4j + Circuit Breaker |
| **캐시 히트율** | 0% | 85% | Redis 다층 캐싱 |
| **에러율** | 15% | <1% | 자동 재시도 + Fallback |

## 🎯 핵심 메시지

**단일 기술이 아닌 통합 솔루션!**
1. **Spring AI**: AI 호출 현대화 (복잡성 70% 감소)
2. **Redis**: 성능 100배 향상 (캐싱)
3. **Kafka**: 사용자 대기 시간 0 (이벤트 드리븐)
4. **비동기 Controller**: 시스템 처리량 50배 증가
5. **Resilience4j**: 99.9% 가용성 확보

**6주 만에 토스뱅크급 아키텍처 구축** 🚀

이렇게 하면 단순한 HTTP 호출 개선이 아니라, **완전한 현대식 리액티브 아키텍처**로 전환할 수 있습니다!