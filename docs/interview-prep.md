# 면접 대비 정리

## 1. 전체 아키텍처 & 기술 스택
- **언어/프레임워크**: Kotlin + Spring Boot 3.5
- **웹 계층**: Spring MVC, Validation, Swagger(OpenAPI)
- **데이터 계층**: Spring Data JPA + JDSL(Querydsl)로 복잡한 조인/페이징 처리
- **보안**: Spring Security, JWT 인증, OAuth2 (Google 등)
- **외부 연동**: Spring AI(OpenRouter) + WebClient 비동기 호출
- **캐시**: Spring Cache + Redis(Lettuce)
- **인프라**: Gradle, Docker, Railway(애플리케이션 & Redis 분리 배포)
- **품질**: ktlint, detekt, Mockk 기반 테스트

## 2. 주요 설계 포인트
- **API 구조**: Controller → Service → Repository. DTO 변환은 Service 또는 Custom Repository에서 처리.
- **JPA 활용**: 엔티티와 DTO를 분리하고, JDSL `selectNew`로 N+1 해결 및 페이징 효율화.
- **Redis 캐시**: 세션 목록(`user-sessions`)과 메시지 목록(`session-messages`)에 TTL을 적용하여 DB 부하 감소.
- **보안 흐름**: JWT 필터 + OAuth2 로그인 → 최초 로그인 시 사용자 생성 후 캐싱.
- **AI 연동**: Spring AI `ChatClient` 사용, 예외 시 fallback 메시지 생성.## 3. 캐싱 트러블슈팅 스토리
1. **초기 문제**: `@Cacheable`이 `PageImpl`을 그대로 Redis에 저장 → 역직렬화 시 `LinkedHashMap`으로 복원되어 `ClassCastException` 발생.
2. **대응**: 캐시 전용 DTO `CachedPage` 도입(`content`, `totalElements`만 보관)으로 Page 직렬화 문제 해결.
3. **두 번째 이슈**: Redis에서 꺼낸 JSON에 타입 메타데이터가 빠져 역직렬화 실패 → `CachedPage`에 `@JsonTypeInfo` 추가.
4. **세 번째 이슈**: Jackson `PolymorphicTypeValidator`가 DTO 패키지 허용 안 함 → `CacheConfig`에서 `BasicPolymorphicTypeValidator`로 허용 범위 지정, `KotlinModule`, `JavaTimeModule` 등록.
5. **피드백**: Redis 키 삭제 후 재검증, TTL 유지(세션 60s / 메시지 30s). 재배포 시 Serializer 설정이 반영됐는지 확인.
6. **결과**: 재진입 시 캐시 히트가 안정적으로 동작하고, 캐시 미스 시 DB → 캐시 저장 → 반환 흐름 확립.

## 4. 예상 면접 질문 & 답변 포인트
- **왜 JPA/JDSL?** 엔티티 중심 설계 + 타입 안전한 쿼리, N+1 해결 경험.
- **Redis 캐시 전략?** 캐시 키 설계, TTL, `@CacheEvict`, DTO 직렬화 개선 과정 설명.
- **Jackson 에러 어떻게 해결?** 로그 분석 → 타입 정보 누락 → ObjectMapper 다형성 설정 → Redis 초기화 순서.
- **Spring Security 구성?** JWT 필터, OAuth 로그인 처리, CORS 정책.
- **AI 연동 안정성?** Fallback 메시지, 응답 JSON 파싱, 예외 로깅.
- **테스트 전략?** Mockk, Controller 통합 테스트, ktlint/detekt 사용.
- **배포 파이프라인?** Gradle 빌드 → Docker 이미지 → Railway 배포, 환경 변수 관리.
- **성능/모니터링?** Actuator health, Redis health check 해결 경험, DB 인덱스 및 페이징 최적화.

## 5. 추가 체크리스트
- Redis 재배포 시 기존 키 삭제(또는 `FLUSHALL`).
- `CacheConfig`, `CachedPage` 최신 상태로 배포되어 있는지 git commit 확인.
- 캐시 키(`user-sessions`, `session-messages`)가 TTL 후 갱신되는지 로그로 확인.
- detekt/ktlint 잔여 경고 중 면접에서 질문 받을 만한 부분(파일명 불일치, throw count 등) 정리.
- OAuth/보안 흐름, Spring AI 응답 구조에 대한 설명 준비.
## 4. 예상 면접 질문 & 상세 답변 전략

### Q1. 왜 Spring Data JPA + JDSL을 선택했나요? 대안과 비교해 설명해 주세요.
- **핵심 의도**: 엔티티 중심 도메인 설계를 유지하면서도 복잡한 조회 로직을 타입 안전하게 작성하고 싶었습니다.
- **JPA 장점**
  1. **엔티티 중심 설계**: 세션, 메시지, 캐릭터 등 도메인 모델을 객체로 표현하고 연관 관계를 명확하게 유지할 수 있습니다. DTO로 매핑하기 전까지 비즈니스 로직을 엔티티 기반으로 작성 가능.
  2. **지연 로딩 + 캐시**: 필요한 데이터만 가져오도록 Lazy fetch, 2차 캐시 설정 옵션 제공.
  3. **퍼시스턴스 컨텍스트**: 변경 감지(dirty checking)를 활용해 최소한의 SQL로 업데이트.
- **JDSL(Querydsl) 사용 이유**
  1. 복잡한 필터링/정렬/페이징 로직을 DSL로 작성하면 컴파일 타임에 오류를 잡을 수 있습니다.
  2. `selectNew<SessionListResponse>` 등 DTO로 바로 매핑해 N+1 문제를 한 번에 해결했습니다.
- **대안**
  1. **MyBatis**: SQL을 직접 제어 가능하지만 XML/어노테이션 관리 부담, 타입 안전성 부족.
  2. **Spring Data JDBC**: 가볍지만 연관 관계 관리 미흡.
  3. **순수 JDBC**: 유연하지만 생산성이 떨어지고 중복 코드가 많습니다.
- **우리 프로젝트에서의 적용**
  - `ChatSessionRepositoryImpl`에서 JDSL로 캐릭터 join + 정렬 + DTO 매핑을 한 번에 처리.
  - 요청 파라미터별 조건(북마크, 종료 여부)을 DSL에서 `where(and(...))`로 동적으로 조합.
  - JPA와 Redis 캐시를 조합해 DB 부하를 줄이고 캐시 미스 시에도 안전하게 페이징 가능.

**후속 질문 대비**
- Q: 데이터가 매우 복잡하거나 Native SQL 성능이 더 좋다면? → 필요 시 `EntityManager`로 Native Query 사용 가능하며, 프로파일링 통해 좁혀나갈 계획.
- Q: 대규모 트래픽 시 JPA 성능 문제? → 쿼리 캐시, Redis 캐시, DB 인덱스 최적화, 필요하면 CQRS 및 전문 쿼리 도입.
### Q2. Redis 캐시를 어떻게 설계했고, 왜 필요했나요?
- **도입 배경**: 세션 목록/메시지 목록 API가 사용자별로 호출 빈도가 높고, DB 조회 시 조인 + 정렬 비용이 큽니다. DB 부하를 줄이고 응답 속도를 보장하기 위해 캐시 사용.
- **키 전략**: `user-sessions::user:{userId}:b:{bookmarked}:c:{isClosed}:p:{page}:s:{size}`, `session-messages::session:{sessionId}:p:{page}:s:{size}`.
- **TTL**: 세션 목록 60초, 메시지 목록 30초로 짧게 유지해 최신 데이터와 성능을 균형화.
- **캐시 로직**: 컨트롤러→서비스→`ChatSessionCacheService`
  1. `@Cacheable` → 캐시 히트 시 `CachedPage` 반환.
  2. 캐시 미스 시 DB 조회(JDSL) → `CachedPage`에 담아 리턴.
  3. Spring Cache가 반환값을 Redis에 직렬화하여 저장.
- **트러블슈팅 요약**
  1. `PageImpl` 직렬화 실패 → `CachedPage` DTO 도입.
  2. `@class` 누락 → `CachedPage`에 `@JsonTypeInfo` 부착.
  3. Jackson PolymorphicTypeValidator가 DTO 패키지를 거부 → `CacheConfig`에서 허용 범위 확장.
  4. Redis 초기화/재배포 시 기존 키 삭제 필요. 설정 반영 후 테스트.
- **대안**: 캐시 미사용 또는 DB 인덱스 튜닝만으로는 피크 부하 대응이 어려웠고, 로컬 in-memory 캐시는 다중 인스턴스 환경에서 일관성이 깨짐.

**후속 질문 대비**
- Q: 캐시 갱신 전략? → 데이터 변경 시 `@CacheEvict(allEntries = true)` 적용. TTL 짧게 두어 eventual consistency 확보.
- Q: Redis 장애 시? → 캐시 미스 처리 code는 DB에서 데이터를 조회하므로 기능은 지속. 향후 circuit breaker나 Redis health 검사(Actuator) 활용 가능.
- Q: 캐시 키 관리? → `@Cacheable` key SpEL로 일관성 유지. 필요 시 `CacheManager`에서 prefix 지정 가능.
### Q3. Spring Security와 OAuth2는 어떻게 구성했나요?
- **JWT 인증 흐름**
  1. `JwtAuthenticationFilter`가 `Authorization` 헤더 검사 → 토큰 파싱 및 Claims 검증.
  2. 토큰 유효 시 `SecurityContext`에 Authentication 저장, 이후 컨트롤러에서 `@AuthenticationPrincipal` 사용.
  3. 토큰 없는 요청은 `AnonymousAuthenticationFilter`가 처리.
- **OAuth2 로그인**
  1. Spring Security OAuth2 Client 설정으로 Google 등 provider 등록, Redirect URI 관리.
  2. OAuth 로그인 성공 → `UserService.findOrCreateOAuthUser`로 사용자 정보 저장/갱신.
  3. JWT 발급 후 클라이언트에 반환, 이후 동일한 JWT 필터 흐름 사용.
- **보안 정책**
  - CSRF 비활성화(REST API), CORS 허용 도메인 설정.
  - 세션 관리: Stateless (JWT 기반), 필요 시 Refresh Token 전략 가능.

**후속 질문 대비**
- Q: JWT 만료/재발급 전략? → Access Token 짧게, Refresh Token or 재로그인 유도. DB에 token blacklist 저장 가능.
- Q: OAuth provider 확장 계획? → Spring Security config에 provider 추가, `spring.security.oauth2.client` 설정만 변경하면 OIDC provider 추가 가능.

### Q4. Spring AI/OpenRouter 연동에서 고려한 점은?
- Spring AI `ChatClient`로 시스템 prompt + user 메시지를 조합, JSON 형식 요구.
- 응답 파싱 실패 대비 fallback 메시지(`handleAiError`) 구현.
- OpenRouter 모델/temperature는 환경변수로 주입.
- 호출 실패/타임아웃 시 로그 남기고 예외 처리.

**후속 질문 대비**
- Q: 비용/지연 시간 최적화는? → 캐시로 AI 반복 호출 줄임, prompt 텍스트 최소화, 모델 선택.
- Q: 안정성? → 예외 시 기본 메시지 제공, 로그 추적, 추후 재시도 로직 도입 검토.
### Q5. 테스트 전략과 품질 관리는 어떻게 하고 있나요?
- **테스트**
  - Service 레벨: Mockk/Mockito로 의존성 주입 후 비즈니스 로직 검증.
  - Controller 레벨: MockMvc/RestDocs (실제 프로젝트 구조에 맞춰 설명)로 요청/응답 시나리오 테스트.
  - Redis 연동 테스트 필요 시 Testcontainers 또는 @DataRedisTest 활용 방안 검토.
- **정적 분석**
  - ktlint: 코드 스타일 유지, CI에서 자동 검사.
  - detekt: 복잡도/중복/unused 코드 경고. 기존 위반사항(파일명-클래스명 불일치 등) 정리 계획 공유.
- **로깅/모니터링**
  - Logback, GlobalExceptionHandler로 에러 로깅.
  - Spring Actuator health (DB, Redis) 확인.

**후속 질문 대비**
- Q: detekt 경고 해결 계획? → 현재 목록 공유, 중요 경고부터 순차 처리.
- Q: 인수 테스트 필요성? → 핵심 API는 통합 테스트(Latest Chat Session, Message Flow)를 우선, E2E는 QA 워크플로우에서 추진.

### Q6. 배포와 인프라는 어떻게 구성했나요?
- **배포 파이프라인**: Gradle → Docker 이미지(publishing) → Railway 배포.
- **환경 변수**: `REDIS_URL`, `JWT_SECRET`, `OPENROUTER_API_KEY` 등 Railway Dashboard 관리.
- **Redis**: Railway 별도 서비스를 사용, 비밀번호 포함 URL로 연결.
- **Logging/Monitoring**: Railway Logs, Redis 재시작 경고 대응 (overcommit). 필요 시 Logback → 외부 수집 연동 고려.

**후속 질문 대비**
- Q: 무중단 배포? → Railway 단일 인스턴스라 Rolling은 불가, 빠른 롤백 준비.
- Q: Redis 장애 대응? → 장애 시 캐시 미스 처리로 DB fallback, Alert 설정 필요.
### Q7. 성능 최적화와 실패 처리 경험은 무엇인가요?
- **성능**
  - Redis 캐시 도입으로 세션/메시지 조회 응답시간 단축.
  - JDSL DTO projection으로 N+1 제거 → DB 쿼리 횟수 감소.
  - DB 인덱스: `lastMessageAt`, `userId` 등 자주 사용하는 필드에 인덱스 적용 (설계 시 설명).
- **트러블슈팅 경험**
  - `PageImpl` → `CachedPage` → Jackson 타입 설정까지 단계별 개선.
  - Redis 재배포 시 FLUSHALL, 환경 변수 재확인.
  - Redis 연결 장애(Lettuce reconnected 로그) → 재시도 로직, 타임아웃 모니터링.
  - Spring Actuator에서 Redis health 초기 실패 → `@Configuration(proxyBeanMethods=false)`로 CGLIB 문제 해결.

**후속 질문 대비**
- Q: 좀 더 큰 트래픽 대비? → Redis cluster/skipping Cacheable, DB sharding, CQRS 구조.
- Q: 장애 전파? → 예외 핸들링, GlobalExceptionHandler에서 사용자 친화적 에러 메시지 제공.

### Q8. 향후 개선하고 싶은 부분은?
- Redis 캐시 키 관리 자동화, Cache warm-up 도입.
- detekt/ktlint 경고 지속 정리. 테스트 커버리지 확대.
- Spring Security에서 Refresh Token + Reissue 로직 추가.
- AI 응답 모니터링(성공률, latency) 대시보드 도입.
- Redis 접속 상태 모니터링(Alerting)과 재시도 정책 강화.
#### JDSL(Querydsl Kotlin DSL) 선택 이유와 장단점
- **Querydsl 대신 JDSL을 선택한 배경**
  1. **Kotlin 친화성**: 전통적인 Querydsl은 `Q` 클래스 생성을 위해 kapt/annotation processing 설정이 필요하고, Kotlin에서는 빌드 속도와 설정이 번거롭습니다. JDSL은 Kotlin DSL로 작성되기 때문에 별도 코드 생성 없이 바로 사용할 수 있어요.
  2. **JPA와 자연스러운 통합**: Line Corp가 제공하는 JDSL은 Spring Data JPA와 연동되도록 설계돼 있어 `findPage` 같은 헬퍼 메서드로 페이징 처리까지 한 번에 해결할 수 있습니다.
  3. **DSL 표현력**: Kotlin의 람다/확장 함수를 활용해 `selectNew<DTO> { ... }`, `where { and(...) }`처럼 가독성 좋은 쿼리를 작성할 수 있습니다.

- **장점**
  - 타입 안전한 쿼리: 컴파일 타임에 필드명을 확인하므로 런타임 에러가 줄어듭니다.
  - Kotlin 프로젝트에 쉽게 녹아듦: kapt 없이 순수 Kotlin DSL로 사용 가능.
  - DTO Projection 지원: `selectNew<SessionListResponse>` 등으로 직접 DTO에 매핑.

- **단점 / Trade-off**
  - 생태계 / 자료가 Querydsl보다 적어 학습 자료가 부족할 수 있습니다.
  - 몇몇 복잡한 SQL 함수나 DB별 특수 기능은 직접 Expression을 작성해야 합니다.
  - Querydsl에 비해 레퍼런스가 적어 팀 합류 시 사전 공유 필요.

- **실제 적용 사례**
  - `ChatSessionRepositoryImpl`에서 `selectNew`와 `join`을 이용해 세션과 캐릭터를 한 번에 조회하고, 정렬/페이징을 DSL로 표현했습니다.
  - 북마크/종료 상태 파라미터를 `where` 조건에 동적으로 넣어 if/else 없이 처리했습니다.
