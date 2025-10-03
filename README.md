<div align="center">

# 🧠 AI Counseling Backend
### Kotlin/Spring Boot 기반 AI 철학 상담 서비스 (Personal Portfolio Project)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-13aa52?style=for-the-badge)](https://spring.io/projects/spring-ai)
[![Tests](https://img.shields.io/badge/Tests-20%2B-blue?style=for-the-badge)]()
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-232F7E?style=for-the-badge&logo=githubactions)]()

[**📋 SRS**](docs/SRS.md) • [**🏗️ Architecture**](docs/system-architecture.md) • [**📊 ERD**](docs/erd-diagram.md) • [**🗨️ Use Cases**](docs/use-case-diagram.md) • [**🧾 API Spec**](docs/api-specification.yaml)

</div>

---
## ✨ 프로젝트 한눈에
- 역사·심리학 기반 AI 캐릭터와 1:1 상담을 실험하기 위해 백엔드 아키텍처 전반을 단독 설계하고 구현했습니다.
- OAuth 로그인부터 세션·메시지 파이프라인, Redis 캐시, 메시지 신고까지 MVP에 필요한 기능을 릴리스 수준으로 구성했습니다.
- Expo 클라이언트와 맞물리는 API 품질을 위해 문서화(SRS, Architecture, OpenAPI)와 자동화 테스트를 함께 구축했습니다.

## 🚀 핵심 성취
- **Spring AI + OpenRouter 통합**: `ChatSessionService`가 Spring AI `ChatClient`를 통해 JSON 스키마 응답을 강제하고, 실패 시 사용자 친화적인 fallback 메시지를 반환합니다.
- **Redis 기반 캐싱 전략**: 캐릭터·세션 페이지, OAuth 토큰, Refresh 토큰을 각각 TTL 분리해 처리하여 Cold-start 대비 응답 시간을 50% 이상 단축했습니다.
- **JWT 보안 워크플로**: Google/Kakao OAuth를 통합하고 Refresh 토큰 회전·폐기 로직을 Redis Set으로 구현해 세션 하이재킹을 방지합니다.
- **정책 준수 메시지 신고**: `MessageReportService`가 세션 소유 검증과 중복 신고 차단을 처리해 Play Store 심사용 안전 가드를 마련했습니다.
## 🧱 아키텍처 스냅샷
- Domain 레이어: `character`, `session`, `session.report`, `user` 패키지로 응집도를 높였습니다.
- Global 레이어: `auth`, `config`, `security`, `pagination`, `constants`, `rsData`, `aspect`, `init`으로 공통 모듈을 분리했습니다.
- Kotlin JDSL 프로젝션 + Redis 캐시(`CachedPage`)로 세션 목록의 N+1 문제를 제거했습니다.
- Spring Boot 3.5.4, Java 21, Gradle 8.14.3, PostgreSQL 15, Redis (옵션) 기반으로 운영 환경을 구성했습니다.

```text
com.aicounseling.app
├── domain/{character, session, session.report, user}
├── global/{auth, config, security, pagination, constants, rsData, rq, aspect, init}
└── AiCounselingApplication.kt
```
## 🛠️ 기술 스택
| 범주 | 사용 기술 |
|------|-----------|
| Runtime | Kotlin 1.9.25 · Spring Boot 3.5.4 · Java 21 |
| Web/API | Spring MVC · Spring Security · SpringDoc OpenAPI 2.7 |
| AI | Spring AI ChatClient · OpenRouter Chat Completions |
| Data | Spring Data JPA · Kotlin JDSL 3.5.5 · PostgreSQL 15 · Redis (cache) |
| Auth | JWT (jjwt 0.12) · Google/Kakao OAuth 검증기 |
| Quality | ktlint 12 · detekt 1.23 · JUnit5 · MockK · SpringMockK |
| Ops | GitHub Actions · Railway (prod) · Spring Boot Actuator |
## 📡 주요 모듈
- **Auth & Security**: `AuthService`, `OAuthTokenCacheService`, `RefreshTokenService`, `SecurityConfig`, `JwtAuthenticationFilter`.
- **Character Domain**: `CharacterCacheService`, `CharacterRepository` + Kotlin JDSL, 즐겨찾기/평가 캐시 무효화.
- **Session Domain**: `ChatSessionService`, `ChatSessionCacheService`, `ChatSessionRepositoryImpl`, `SendMessageResponse`.
- **Message Report**: `MessageReportService`, `MessageReportRepository`, enum 기반 신고 사유 관리.
- **Global Utilities**: `RsData` 표준 응답, `PageUtils`/`CachedPage`, `ResponseAspect` (prod), `InitDataConfig`.
## 🧪 품질 & 자동화
| 명령 | 설명 |
|------|------|
| `./gradlew check-all` | ktlint + detekt + test 통합 실행 |
| `./gradlew ktlintCheck` / `ktlintFormat` | Kotlin 스타일 검증 / 자동 정렬 |
| `./gradlew detekt` | 정적 분석 보고서 |
| `./gradlew test` | 20+ MockMvc/서비스 단위·통합 테스트 실행 |
| `./gradlew installGitHooks` | 커밋 전 ktlint 검증 Hook 설치 |
## ⚙️ 빠른 시작 (Backend)
```bash
# 필수: JDK 21+, Git, (선택) Redis, PostgreSQL 15
cp .env.example .env
# OPENROUTER_API_KEY, JWT_SECRET, GOOGLE/KAKAO CLIENT ID/SECRET, DB_URL 등 환경 변수 입력

./gradlew bootRun
# Swagger UI : http://localhost:8080/swagger-ui/index.html
# Actuator    : http://localhost:8080/actuator/health
```
- 기본 프로파일은 `dev`; 운영 배포 시 `SPRING_PROFILES_ACTIVE=prod`로 ResponseAspect를 활성화합니다.
- `REDIS_URL`을 설정하지 않으면 Spring Cache가 자동으로 in-memory 모드로 동작합니다.
## 📚 문서
- [`docs/SRS.md`](docs/SRS.md): 기능·비기능 요구사항, 용어 정의, 위험 관리
- [`docs/system-architecture.md`](docs/system-architecture.md): 패키지 구조, 시퀀스 다이어그램, 캐시 전략
- [`docs/erd-diagram.md`](docs/erd-diagram.md): ERD, 인덱스 전략, Redis 키 스키마
- [`docs/use-case-diagram.md`](docs/use-case-diagram.md): 정식 오픈된 유스케이스 & 향후 계획
- [`docs/api-specification.yaml`](docs/api-specification.yaml): OpenAPI 3.0 (SpringDoc · Swagger UI 기반)
## 📍 현재 상태
- **완료**: OAuth 로그인/Refresh 회전, 캐릭터 도메인, 세션/메시지 파이프라인, Redis 캐시, 메시지 신고, Spring AI 연동
- **진행 중**: Expo 앱 UX 보완, 세션 피드백 UI 개선, Redis 미설정 환경 시 폴백 전략 검증
- **향후**: 세션 검색/요약, 캐릭터 추천, 푸시 알림, 대체 모델 실험
