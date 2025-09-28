# Redis 캐시 설계 및 구현 가이드

## 1. 캐시 대상 선정
- **세션 목록**: 사용자 대시보드에서 자주 조회하지만 변경 빈도가 낮은 데이터
- **메시지 목록**: 단일 세션 내 채팅 기록. 메시지가 추가될 때만 변경됨

### 왜 캐시가 필요한가?
- JDSL로 최적화된 쿼리라도 세션 수가 많아지면 DB 부하 상승
- 사용자별/세션별로 동일 데이터 재요청 → Redis 캐시로 응답 시간을 일정하게 유지

## 2. 키 설계
```text
user-sessions::user:{userId}:b:{bookmarked}:c:{isClosed}:p:{page}:s:{size}
session-messages::session:{sessionId}:p:{page}:s:{size}
```
- 필터·페이징 정보를 키에 포함해 충돌 방지
- prefix(`user-sessions`, `session-messages`)로 캐시 종류 구분

## 3. TTL 전략
- 세션 목록: 60초
- 메시지 목록: 30초
- TTL 짧게 유지 → 데이터 신선도 확보 + stale state 자동 해소

## 4. 캐시 저장/조회 흐름
```mermaid
graph TD;
    A[요청: 세션 목록] --> B{@Cacheable
user-sessions}
    B -- 히트 --> H[Redis에서 CachedPage 반환]
    B -- 미스 --> C[DB 조회 (JDSL)]
    C --> D[CachedPage.from(Page)]
    D --> E[Redis 저장]
    D --> F[PageImpl으로 변환 후 응답]
```

### 구현 포인트
- `ChatSessionCacheService`가 `@Cacheable` 메서드에서 `CachedPage` DTO 반환
- `ChatSessionService`가 `CachedPage.toPage(pageable)` 호출해 Controller로 전달
- `@CacheEvict`로 캐시 무효화: 세션 시작/종료, 메시지 발송 시 해당 키 삭제

## 5. CachedPage DTO
```kotlin
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@class",
)
data class CachedPage<T>(
    val content: List<T>,
    val totalElements: Long,
) {
    fun toPage(pageable: Pageable): Page<T> = PageImpl(content, pageable, totalElements)

    companion object {
        fun <T> from(page: Page<T>): CachedPage<T> =
            CachedPage(
                content = page.content,
                totalElements = page.totalElements,
            )
    }
}
```
- PageImpl 직렬화 문제를 피하기 위해 캐시 전용 DTO 사용
- `@JsonTypeInfo`로 루트에 타입 정보 추가 → Redis에서 타입이 보존되도록 함

## 6. ObjectMapper 설정 (CacheConfig)
```kotlin
private fun createRedisSerializer(): GenericJackson2JsonRedisSerializer {
    val mapper = (objectMapper ?: ObjectMapper()).copy()

    val typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType("com.aicounseling.app")
            .allowIfBaseType("org.springframework.data.domain")
            .allowIfBaseType("java.util")
            .allowIfBaseType("java.time")
            .allowIfBaseType("java.lang")
            .build()

    mapper.registerModule(KotlinModule.Builder().build())
    mapper.registerModule(JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    @Suppress("DEPRECATION")
    mapper.activateDefaultTyping(
        typeValidator,
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY,
    )

    return GenericJackson2JsonRedisSerializer(mapper)
}
```
- 캐시 값 역직렬화 시 `LinkedHashMap`으로 떨어지지 않게 DTO/컬렉션/시간 타입을 상위 클래스로 허용
- Kotlin module + JavaTimeModule 등록

## 7. spring-cache 설정
'tcacheManager`에 캐시 TTL 적용
defaultConfig.entryTtl(DEFAULT_CACHE_TTL)
12
    val cacheConfigurations =
        mapOf(
            "user-sessions" to defaultConfig.entryTtl(USER_SESSION_CACHE_TTL),
            "session-messages" to defaultConfig.entryTtl(SESSION_MESSAGES_CACHE_TTL),
        )
```
- default TTL은 10분, 세션/메시지는 각 60s/30s로 override

## 8. 캐시 무효화(@CacheEvict)
```kotlin
@CacheEvict(cacheNames = ["user-sessions"], allEntries = true)
fun startSession(...)

@Caching(
    evict = [
        CacheEvict(cacheNames = ["user-sessions"], allEntries = true),
        CacheEvict(cacheNames = ["session-messages"], allEntries = true),
    ],
)
fun sendMessage(...)
```
- 세션 상태·메시지가 변할 때 해당 캐시를 삭제
'tallEntries = true` → 필터나 페이지 구분 없이 전부 삭제 → TTL이 짧아서 부담이 적음

## 9. 테스트 & 배포 시 주의
1. Redis 재배포 시 기존 키 삭제(FLUSHDB/FLUSHALL)
2. Jackson 설정이 변경되면 직렬화 오류가 발생할 수 있으니, 꼭 JSON 구조 확인
3. Redis 장애 대비: 캐시 미스 시 DB 조회로 fallback 하도록 설계되어 있음 (완전한 다운 대비는 추후 재시도/서킷 브레이커 도입 검토)

## 10. 차후 개선 아이디어
- 캐시 Pre-warm(사용자 로그인 시 세션 목록 미리 로드)
- 캐시 히트율/미스율 모니터링, Redis Cluster 도입 검토
- 캐시 키 자동 관리 도구 제작(키 리스트 + TTL 확인)