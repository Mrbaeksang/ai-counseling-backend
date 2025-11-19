# 면접 대비 Q&A (개인 프로젝트)

- [1. 기술 스택 선정과 아키텍처](#1-기술-스택-선정과-아키텍처)
  - [Q1. 왜 Kotlin + Spring Boot를 사용했나요?](#q1-왜-kotlin--spring-boot를-사용했나요)
  - [Q2. 왜 Spring Data JPA + JDSL을 선택했나요?](#q2-왜-spring-data-jpa--jdsl을-선택했나요)
- [2. 데이터베이스 & 성능](#2-데이터베이스--성능)
  - [Q3. DB 설계와 성능 최적화는 어떻게 했나요?](#q3-db-설계와-성능-최적화는-어떻게-했나요)
  - [Q4. Redis 캐시는 왜 도입했고 어떻게 설계했나요?](#q4-redis-캐시는-왜-도입했고-어떻게-설계했나요)
- [3. 캐시 트러블슈팅](#3-캐시-트러블슈팅)
  - [Q5. Redis 역직렬화 문제를 어떻게 해결했나요?](#q5-redis-역직렬화-문제를-어떻게-해결했나요)
- [4. 보안 및 인증](#4-보안-및-인증)
  - [Q6. Spring Security와 JWT 구조는 어떻게 되나요?](#q6-spring-security와-jwt-구조는-어떻게-되나요)
- [5. AI 연동](#5-ai-연동)
  - [Q7. Spring AI(OpenRouter) 연동은 어떻게 했나요?](#q7-spring-aiopenrouter-연동은-어떻게-했나요)
- [6. 테스트와 배포](#6-테스트와-배포)
  - [Q8. 테스트와 코드 품질은 어떻게 관리하나요?](#q8-테스트와-코드-품질은-어떻게-관리하나요)
  - [Q9. 배포와 인프라는 어떻게 구성했나요?](#q9-배포와-인프라는-어떻게-구성했나요)
- [7. 성능 최적화 & 향후 계획](#7-성능-최적화--향후-계획)
  - [Q10. 성능 개선과 향후 확장 계획은?](#q10-성능-개선과-향후-확장-계획은)
- [부록 A1. Kotlin + Spring Boot 선택 이유 (상세)](#부록-a1-kotlin--spring-boot-선택-이유-상세)
- [부록 A2. QueryDSL과 JDSL 비교 정리 (상세)](#부록-a2-querydsl과-jdsl-비교-정리-상세)
- [부록 A3. Kotlin Null-Safety & 스코프 함수 정리](#부록-a3-kotlin-null-safety--스코프-함수-정리)
- [부록 A4. DB 설계 & 성능 튜닝 상세](#부록-a4-db-설계--성능-튜닝-상세)
- [부록 A5. Redis 캐시 설계 상세](#부록-a5-redis-캐시-설계-상세)
- [부록 A6. Redis 트러블슈팅 상세](#부록-a6-redis-트러블슈팅-상세)
- [부록 A7. Spring Security & JWT 구성 상세](#부록-a7-spring-security--jwt-구성-상세)
- [부록 A8. Spring AI(OpenRouter) 연동 상세](#부록-a8-spring-aionrouter-연동-상세)
- [부록 A9. 테스트 & 품질 전략 상세](#부록-a9-테스트--품질-전략-상세)
- [부록 A10. 배포 & 인프라 구성 상세](#부록-a10-배포--인프라-구성-상세)
- [부록 A11. 성능 최적화 & 향후 계획 상세](#부록-a11-성능-최적화--향후-계획-상세)

---

## 1. 기술 스택 선정과 아키텍처

### Q1. 왜 Kotlin + Spring Boot를 사용했나요?
**짧은 답변**  
혼자 4주 안에 백엔드를 완성해야 해서 생산성이 높은 스택이 필요했습니다. Spring Boot는 실무에서 가장 많이 쓰이는 프레임워크라 생태계가 풍부하고, Kotlin은 Null-Safety와 간결한 문법 덕분에 JPA/DTO 작업이 훨씬 수월했습니다. Java와 100% 호환되면서도 추가 편의 기능을 제공한다는 점도 장점입니다. 자세한 이유는 [부록 A1](#부록-a1-kotlin--spring-boot-선택-이유-상세) 참고.

### Q2. 왜 Spring Data JPA + JDSL을 선택했나요?
**짧은 답변**  
ORM 기능은 검증된 Spring Data JPA(Hibernate)가 맡고, 복잡한 조회는 Kotlin 친화적 DSL인 JDSL로 구현했습니다. QueryDSL도 고려했지만 Kotlin에선 Q 클래스 생성을 위한 `kapt` 설정과 빌드 단계가 번거롭고 Java 스타일 체이닝 문법이라 가독성이 떨어졌습니다. JDSL은 `selectNew<DTO> { ... }`, `where { and(...) }`처럼 Kotlin 문법으로 작성할 수 있어 조건 추가/제거가 쉽고, 별도 코드 생성 없이 바로 사용할 수 있었습니다. 실제로 `ChatSessionRepositoryImpl`에서 `selectNew<SessionListResponse>`와 `join(Character::class)` 조합으로 N+1을 제거했습니다. 비교 상세는 [부록 A2](#부록-a2-querydsl과-jdsl-비교-정리-상세) 참고.

## 2. 데이터베이스 & 성능

### Q3. DB 설계와 성능 최적화는 어떻게 했나요?
**짧은 답변**  
PostgreSQL을 사용해 세션/메시지/캐릭터 테이블을 정규화했고, `lastMessageAt`, `userId`, `sessionId`에 인덱스를 적용했습니다. JDSL로 필요한 컬럼만 조회해서 Select 비용을 줄였고, join + DTO projection으로 N+1 문제를 해소했습니다. 자세한 설계는 [부록 A4](#부록-a4-db-설계--성능-튜닝-상세) 참고.

### Q4. Redis 캐시는 왜 도입했고 어떻게 설계했나요?
**짧은 답변**  
세션/메시지 목록은 호출 빈도가 높아 캐시를 붙여 DB 부하와 응답 시간을 줄였습니다. 키에는 userId, 필터, 페이지 정보를 포함해 충돌을 방지했고, TTL은 세션 60초·메시지 30초로 짧게 설정했습니다. `@Cacheable`로 캐시 미스 시 DB 조회 → 자동 저장 → 이후 히트 시 즉시 반환 흐름을 유지합니다. 상세 설계는 [부록 A5](#부록-a5-redis-캐시-설계-상세) 참고.

## 3. 캐시 트러블슈팅

### Q5. Redis 역직렬화 문제를 어떻게 해결했나요?
**짧은 답변**  
처음엔 `PageImpl`을 그대로 캐시에 저장했다가 Redis에서 꺼낼 때 `LinkedHashMap`으로 변환돼 `ClassCastException`이 발생했습니다. `CachedPage` DTO를 만들어 캐시하고, JSON 타입 정보 누락은 `@JsonTypeInfo` + ObjectMapper 다형성 설정으로 해결했습니다. 최종적으로 `BasicPolymorphicTypeValidator`에 우리 DTO 패키지를 등록해 역직렬화가 안정적으로 동작하게 했습니다. (세부 과정은 [부록 A5~A6](#부록-a6-redis-트러블슈팅-상세)와 [Redis 캐시 가이드](redis-cache-guide.md) 참고)

## 4. 보안 및 인증

### Q6. Spring Security와 JWT 구조는 어떻게 되나요?
**짧은 답변**  
`JwtAuthenticationFilter`가 Authorization 헤더에서 토큰을 추출해 검증하고, 성공 시 `SecurityContext`에 Authentication을 저장합니다. OAuth 로그인은 Spring Security OAuth2 Client로 처리하고 최초 로그인 시 사용자 정보를 저장 후 JWT를 발급합니다. 현재는 Access Token만 사용하지만 Refresh Token 도입과 토큰 블랙리스트 전략을 검토 중입니다. 자세한 구성은 [부록 A7](#부록-a7-spring-security--jwt-구성-상세) 참고.

## 5. AI 연동

### Q7. Spring AI(OpenRouter) 연동은 어떻게 했나요?
**짧은 답변**  
Spring AI `ChatClient`로 시스템 프롬프트와 사용자 메시지를 조합해 AI 응답을 받습니다. JSON 파싱 실패 시 fallback 메시지를 제공하고 로그를 남겨 추후 장애 분석이 가능하도록 했습니다. 모델·Temperature는 환경 변수로 관리하며, 향후 코루틴을 사용해 여러 AI 호출을 병렬화할 계획입니다. 자세한 설명은 [부록 A8](#부록-a8-spring-aionrouter-연동-상세) 참고.

## 6. 테스트와 배포

### Q8. 테스트와 코드 품질은 어떻게 관리하나요?
**짧은 답변**  
Mockk 기반 테스트로 Service 로직을 검증하고, Controller는 MockMvc로 API 시나리오를 확인했습니다. ktlint와 detekt로 코딩 컨벤션과 정적 분석을 주기적으로 실행했습니다. 잔여 detekt 경고는 리팩토링 계획에 포함돼 있습니다. 상세 전략은 [부록 A9](#부록-a9-테스트--품질-전략-상세) 참고.

### Q9. 배포와 인프라는 어떻게 구성했나요?
**짧은 답변**  
Gradle 빌드 → Docker 이미지 → Railway 배포 순서로 구성했습니다. 애플리케이션과 Redis는 Railway에서 각각의 서비스로 운영하며, 환경 변수(`REDIS_URL`, `JWT_SECRET`, `OPENROUTER_API_KEY`)는 Dashboard에서 관리합니다. GitHub Actions로 main 브랜치 머지 전에 테스트가 자동 실행되도록 했습니다. 상세 구성은 [부록 A10](#부록-a10-배포--인프라-구성-상세) 참고.

## 7. 성능 최적화 & 향후 계획

### Q10. 성능 개선과 향후 확장 계획은?
**짧은 답변**  
JDSL DTO projection과 Redis 캐시로 조회 성능을 최적화했고, 인덱스로 기본적인 DB 튜닝을 적용했습니다. 앞으로는 Redis 캐시 키 관리 자동화, detekt 경고 해결, Refresh Token 도입, AI 응답 모니터링 대시보드 등을 추가해 서비스 품질을 높일 계획입니다. 자세한 계획은 [부록 A11](#부록-a11-성능-최적화--향후-계획-상세) 참고.

---

## 부록 A1. Kotlin + Spring Boot 선택 이유 (상세)

1. **생산성과 친숙함**
   - Spring Boot는 국내 실무에서 가장 많이 쓰이는 백엔드 프레임워크라 생태계가 풍부하고, 혼자서도 빠르게 개발할 수 있는 환경이 마련되어 있습니다.
   - 기존 Java 라이브러리를 거의 수정 없이 사용할 수 있어 러닝커브가 낮습니다.

2. **Kotlin의 Null-Safety**
   - `String` vs `String?`처럼 nullable 여부를 타입에 명시할 수 있어, 런타임 NullPointerException을 상당 부분 예방할 수 있습니다.
   - `let`, `?:`, `?.` 같은 문법 덕분에 null 체크 로직이 단순해지고, 안전한 데이터 검증이 가능합니다.

3. **간결한 문법으로 인한 생산성 향상**
   - `data class` 하나로 `equals/hashCode/toString/copy`를 자동 생성해 DTO와 Value Object를 빠르게 정의합니다.
   - 스코프 함수(`apply`, `also`, `run`)로 객체 초기화/변환 로직을 한눈에 이해할 수 있는 형태로 표현합니다.

4. **함수형 및 DSL 지원**
   - Kotlin 람다·확장 함수 문법 덕분에 Spring Security, Gradle, JDSL 같은 DSL을 자연스럽게 작성할 수 있어 코드 가독성이 높습니다.
   - `when`, `sealed class`, `extension function`을 통해 예외 처리나 상태 표현을 명확하게 할 수 있습니다.

5. **Java와 100% 호환**
   - ClassLoader, Reflection, Lombok 등을 포함해 기존 Java 라이브러리를 그대로 활용할 수 있습니다.
   - 빌드/배포 환경(Gradle, Docker)도 동일하게 사용 가능해 인프라 세팅 부담이 없습니다.

6. **코루틴 도입 여지**
   - 현재는 MVC(블로킹) 구조지만, 향후 WebFlux나 외부 API 병렬 호출이 필요하면 코루틴을 쉽게 도입할 수 있습니다.

### Q1로 돌아가기
[↩ Q1. 왜 Kotlin + Spring Boot를 사용했나요?](#q1-왜-kotlin--spring-boot를-사용했나요)

---

## 부록 A2. QueryDSL과 JDSL 비교 정리 (상세)

### 1) QueryDSL은 어떻게 동작하나요?
1. **엔티티마다 Q클래스 생성**
   - 예시 엔티티 `ChatSession`이 있으면 빌드 시 `QChatSession`이라는 클래스를 자동 생성합니다.
   - Kotlin에서는 `kapt`(Kotlin Annotation Processing Tool)가 annotation processing을 담당합니다.
   - Q 클래스가 있어야 IDE 자동완성과 컴파일 시 타입 체크가 가능합니다.

2. **코드 작성 방식**
   ```kotlin
   val session = QChatSession.chatSession
   val query = queryFactory.selectFrom(session)
       .where(session.userId.eq(userId))
       .fetch()
   ```
   - Q클래스를 이용해 쿼리를 작성하므로 SQL 문자열을 직접 쓰지 않아도 됩니다.

3. **장점**
   - 복잡한 join, subquery도 타입 안전하게 작성할 수 있습니다.
   - Java/Kotlin 모두에서 자료가 많고 레퍼런스가 풍부합니다.

4. **단점 (Kotlin 프로젝트 기준)**
   - `kapt` 설정이 필수라 빌드가 무거워지고 설정이 번거롭습니다.
   - Java 스타일 체이닝 문법이라 Kotlin스러운 가독성을 얻기 어렵습니다.

### 2) JDSL은 무엇이고 어떻게 구현돼 있나요?
1. **Kotlin DSL 형태**
   ```kotlin
   val result = jpaQueryFactory
       .findPage(pageable) {
           selectNew<SessionListResponse>(
               path(ChatSession::id),
               path(ChatSession::characterId),
               path(ChatSession::title),
               path(Character::name),
               path(ChatSession::lastMessageAt)
           )
           from(entity(ChatSession::class))
           join(Character::class).on(path(ChatSession::characterId).eq(path(Character::id)))
           where(
               and(
                   path(ChatSession::userId).eq(userId),
                   bookmarked?.let { path(ChatSession::isBookmarked).eq(it) }
               )
           )
       }
   ```
   - Kotlin 람다·함수 체이닝으로 쿼리를 표현합니다.

2. **kapt가 필요 없음**
   - Kotlin reflection으로 필드 경로를 표현하므로 별도 Q 클래스가 필요 없습니다.
   - 설정이 가볍고 빌드 속도에 영향이 적습니다.

3. **Spring Data JPA와의 통합**
   - `findPage(pageable) { ... }` 같은 확장 함수로 페이징/정렬/DTO 매핑을 한 번에 처리합니다.
   - `selectNew<DTO>`로 필요한 컬럼만 조회해 N+1 문제를 해결합니다.

4. **장단점 요약**
   - 장점: Kotlin 친화적 문법, kapt 불필요, 가독성↑
   - 단점: 생태계가 QueryDSL보다 작아 자료가 적고, 특정 DB 함수는 직접 Expression 작성 필요

### 3) 이번 프로젝트 적용 요약
- `ChatSessionRepositoryImpl`에서 세션과 캐릭터를 join하고 `selectNew<SessionListResponse>`로 DTO 변환
- `where(and(...))`로 필터를 동적으로 조합, `orderBy` + `findPage`로 페이징 처리
- JDSL 덕분에 Kotlin 코드 스타일을 유지하면서도 타입 안전한 쿼리를 구성했습니다.

### Q2로 돌아가기
[↩ Q2. 왜 Spring Data JPA + JDSL을 선택했나요?](#q2-왜-spring-data-jpa--jdsl을-선택했나요)

---

## 부록 A3. Kotlin Null-Safety & 스코프 함수 정리

1. **`?.` (Safe call)** : 왼쪽 값이 null이면 전체 표현식이 null, 아니면 오른쪽 함수를 실행 (`user?.name`).
2. **`?:` (Elvis 연산자)** : 왼쪽 값이 null이면 오른쪽 값 사용 (`title ?: "새 상담"`).
3. **`let`** : `nullableValue?.let { ... }` 형태로 사용하면 null이 아닐 때만 블록 실행.
4. **스코프 함수**
   - `apply` : 블록 안에서 `this`로 자기 자신을 참조, 블록 종료 시 자기 자신 반환 (객체 초기화).
   - `run` : `this`로 접근하지만 블록 결과를 반환 (계산/변환 용도).
   - `also` : `it`으로 접근하고 자기 자신을 반환 (디버깅, 로깅에 활용).

### 예시
```kotlin
val title = chatSession.title?.takeIf { it.isNotBlank() } ?: "새 상담"
chatSession.notes?.let { notes ->
    logger.info("추가 메모: $notes")
}
val session = ChatSession().apply {
    this.userId = userId
    this.characterId = characterId
}
```

### Q1로 돌아가기
[↩ Q1. 왜 Kotlin + Spring Boot를 사용했나요?](#q1-왜-kotlin--spring-boot를-사용했나요)

---

## 부록 A4. DB 설계 & 성능 튜닝 상세

1. **스키마 설계**
   - `chat_session`, `message`, `character`, `user` 테이블을 정규화하고 외래키 제약으로 데이터 무결성을 보장했습니다.
   - `chat_session`-`message`는 1:N, `character`-`chat_session`도 1:N 구조.
   - 삭제/수정 시 일관성을 유지하도록 필요한 곳에 `ON DELETE CASCADE` 적용.

2. **인덱스 전략**
   - 세션 목록: `user_id + is_closed + is_bookmarked + last_message_at` 복합 인덱스.
   - 메시지 목록: `session_id + created_at` 인덱스로 페이징 시 효율 극대화.

3. **쿼리 최적화**
   - JDSL `selectNew<SessionListResponse>`로 필요한 컬럼만 조회.
   - 캐릭터 정보를 join하여 N+1 문제 제거.
   - 정렬을 DB에서 수행 (`orderBy(lastMessageAt.desc(), createdAt.desc())`).

4. **확장 계획**
   - 데이터 증가 시 읽기/쓰기 분리, 리플리카 운영 가능.
   - 통계 요구가 커지면 Materialized View, ETL(Spring Batch) 도입 고려.

### Q3로 돌아가기
[↩ Q3. DB 설계와 성능 최적화는 어떻게 했나요?](#q3-db-설계와-성능-최적화는-어떻게-했나요)

---

## 부록 A5. Redis 캐시 설계 상세

- 캐시 대상, 키 설계, TTL 전략 등 전체 구현 흐름은 별도 문서에 정리했습니다.
- 자세한 내용: [redis-cache-guide.md](redis-cache-guide.md)

### Q4로 돌아가기
[↩ Q4. Redis 캐시는 왜 도입했고 어떻게 설계했나요?](#q4-redis-캐시는-왜-도입했고-어떻게-설계했나요)

---

## 부록 A6. Redis 트러블슈팅 상세

1. **문제 발생** : Redis에서 캐시 값을 꺼낼 때 `LinkedHashMap cannot be cast` 예외 발생.
2. **원인 분석**
   - `PageImpl` 직렬화 → Redis에서 타입 정보가 사라져 복원 불가.
   - Jackson 다형성 설정이 없어 `@class` 메타데이터가 기록되지 않음.
3. **해결 과정**
   1. `CachedPage` DTO 도입 (content + totalElements만 저장).
   2. `@JsonTypeInfo(use = Id.CLASS, property = "@class")`로 타입 정보 기록.
   3. ObjectMapper에 `activateDefaultTyping + BasicPolymorphicTypeValidator` 설정으로 DTO/컬렉션/시간 타입을 허용.
   4. Redis 재배포 시 기존 키 삭제 후 테스트.
4. **결과** : 캐시 히트 시에도 역직렬화가 안정적으로 동작하며, 재입장 시 메시지가 사라지는 문제가 해결됨.
5. **문서 참조** : [redis-cache-guide.md](redis-cache-guide.md)

### Q5로 돌아가기
[↩ Q5. Redis 역직렬화 문제를 어떻게 해결했나요?](#q5-redis-역직렬화-문제를-어떻게-해결했나요)

---

## 부록 A7. Spring Security & JWT 구성 상세

1. **필터 체인**
   - `JwtAuthenticationFilter` → JWT 추출/검증 → `SecurityContext` 저장.
   - `OAuth2AuthorizationRequestRedirectFilter` → 소셜 로그인 처리.
   - `LogoutFilter`, `ExceptionTranslationFilter` 등 Spring Security 기본 필터 유지.

2. **인증 흐름**
   - 로그인 성공 시 Access Token 발급, 응답 헤더/바디로 전달.
   - 인증 필요한 API는 Bearer 토큰 기반으로 호출.
   - Refresh Token은 현재 미사용이지만 Redis 기반 블랙리스트 전략과 함께 도입 예정.

3. **권한 관리**
   - 기본 사용자/관리자 두 역할을 상정, API 별 `hasAuthority` 설정 가능.
   - `SecurityContextHolder`에 사용자 ID 저장 → 컨트롤러에서 `@AuthenticationPrincipal` 대체로 사용.

4. **예외 처리**
   - 인증 실패 → 401 반환, `GlobalExceptionHandler`에서 메시지 통일.
   - 권한 없음 → 403 반환, 로그로 에러 트래킹.

### Q6로 돌아가기
[↩ Q6. Spring Security와 JWT 구조는 어떻게 되나요?](#q6-spring-security와-jwt-구조는-어떻게-되나요)

---

## 부록 A8. Spring AI(OpenRouter) 연동 상세

1. **사용 라이브러리** : `spring-ai-openai-spring-boot-starter`
2. **호출 흐름**
   - `ChatClient` → 시스템 프롬프트 + 사용자 메시지 전달.
   - 모델/temperature 등은 `application.yml`에서 환경 변수로 관리.
3. **응답 파싱**
   - AI가 JSON 형식으로 응답하도록 프롬프트에 형식 요구.
   - 파싱 실패 시 `handleAiError`에서 fallback 메시지 생성, 로그 기록.
4. **예외 처리**
   - 네트워크 오류/타임아웃 → 사용자에게 재시도 안내 메시지.
   - 응답 로그는 1000자 이하로 절단해 저장.
5. **개선 계획**
   - 코루틴을 사용해 다른 분석 API와 병렬 호출.
   - 응답 성공률/지연시간 모니터링 대시보드 구축.

### Q7로 돌아가기
[↩ Q7. Spring AI(OpenRouter) 연동은 어떻게 했나요?](#q7-spring-aiopenrouter-연동은-어떻게-했나요)

---

## 부록 A9. 테스트 & 품질 전략 상세

1. **Unit Test**
   - Mockk/SpringMockK로 Service 레이어 테스트.
   - Repository 단위 테스트는 H2 대신 in-memory PostgreSQL(Testcontainers) 도입 검토.

2. **Integration Test**
   - MockMvc로 Controller API 시나리오 테스트.
   - Spring REST Docs 혹은 Swagger로 문서화.

3. **정적 분석**
   - ktlint: 코드 스타일 자동 검증.
   - detekt: 복잡도, unused import 등 정적 분석. 현재 경고 리스트 관리 중.

4. **품질 지표**
   - 테스트 커버리지, 빌드 실패율, detekt 경고 수를 주기적으로 확인.

### Q8로 돌아가기
[↩ Q8. 테스트와 코드 품질은 어떻게 관리하나요?](#q8-테스트와-코드-품질은-어떻게-관리하나요)

---

## 부록 A10. 배포 & 인프라 구성 상세

1. **빌드/배포 파이프라인**
   - `./gradlew clean build` → Docker 이미지 빌드 → Railway 배포.
   - GitHub Actions: Pull Request 시 ktlint/detekt/테스트 실행, main 머지 후 자동 배포.

2. **환경 변수 관리**
   - `REDIS_URL`, `JWT_SECRET`, `OPENROUTER_API_KEY` 등은 Railway Dashboard Secrets에 저장.
   - 로컬 개발 시 `.env`를 통해 동일 값 로드.

3. **인프라**
   - 애플리케이션과 Redis를 Railway에서 각각의 서비스로 운영.
   - Redis 경고(`vm.overcommit_memory`)는 운영 환경에서 sysctl 설정 필요.

### Q9로 돌아가기
[↩ Q9. 배포와 인프라는 어떻게 구성했나요?](#q9-배포와-인프라는-어떻게-구성했나요)

---

## 부록 A11. 성능 최적화 & 향후 계획 상세

1. **현행 최적화**
   - JDSL DTO projection + Redis 캐시로 조회 성능 최적화.
   - DB 인덱스 정비, 필요한 컬럼만 select.

2. **향후 계획**
   - Redis 캐시 warm-up, 히트율 모니터링.
   - Refresh Token/토큰 블랙리스트 도입, 보안 강화.
   - AI 응답 모니터링 대시보드, 알람 시스템 구축.
   - 대규모 트래픽 대비: 읽기/쓰기 분리, 메시지 큐 도입, CQRS 검토.

### Q10로 돌아가기
[↩ Q10. 성능 개선과 향후 확장 계획은?](#q10-성능-개선과-향후-확장-계획은)
