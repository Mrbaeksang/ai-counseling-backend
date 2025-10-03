# 소프트웨어 요구사항 명세서 (SRS)

## 1. 개요

### 1.1 목적
AI Counseling App의 백엔드와 모바일 클라이언트가 달성해야 하는 기능, 품질, 운영 기준을 정의한다. 개발·QA·운영 단계에서 일관된 참조 지점으로 활용한다.

### 1.2 범위
- 프로젝트명: AI Counseling App
- 플랫폼: Kotlin / Spring Boot 기반 REST API + Expo / React Native 클라이언트
- 핵심 시나리오: 역사·심리학 기반 AI 캐릭터와 1:1 상담, 세션 관리, 평가·신고 및 즐겨찾기

### 1.3 용어 정의
- **캐릭터(Character)**: `characters` 테이블에 저장된 AI 상담 페르소나.
- **세션(Chat Session)**: `chat_sessions` 레코드로 표현되는 사용자-캐릭터 대화 단위.
- **메시지(Message)**: `messages` 테이블에 저장되는 USER/AI 발화.
- **북마크**: 사용자가 다시 보고 싶은 세션에 표시하는 즐겨찾기 상태.
- **RsData**: `resultCode`, `msg`, `data` 삼중 구조로 모든 API가 반환하는 표준 응답 래퍼.
- **OAuth 토큰 캐시**: Google/Kakao 사용자 정보 검증 결과를 Redis에 TTL 기반으로 저장해 API 호출을 절감하는 캐시 계층.
- **메시지 신고**: 사용자 발화 혹은 AI 응답이 정책을 위반했을 때 `message_reports`에 기록하는 기능.
## 2. 기능 요구사항

### 2.1 인증 및 사용자
- **FR-001**: 사용자는 Google OAuth 토큰으로 `POST /api/auth/login/google`에 로그인할 수 있어야 한다.
- **FR-002**: 사용자는 Kakao OAuth 토큰으로 `POST /api/auth/login/kakao`에 로그인할 수 있어야 한다.
- **FR-003**: 로그인 성공 시 백엔드는 Access/Refresh JWT를 발급하고 Redis에 Refresh 토큰을 저장한 뒤 `AuthResponse`를 반환해야 한다.
- **FR-004**: `POST /api/auth/refresh`는 유효한 `refreshToken`으로 새 토큰 쌍을 발급하고 기존 토큰을 회전시켜야 한다.
- **FR-005**: `GET /api/users/me`는 로그인 사용자의 프로필(이메일, 닉네임, 프로필 이미지, 최근 로그인 시각)을 반환해야 한다.
- **FR-006**: `PATCH /api/users/nickname`은 닉네임을 2~20자 문자열로 변경하고 중복을 검증해야 한다.
- **FR-007**: `DELETE /api/users/me`는 계정과 연관된 세션/메시지/평가/북마크/Refresh 토큰을 삭제해야 한다.

### 2.2 캐릭터 (AI 페르소나)
- **FR-010**: `GET /api/characters`는 `sort`(popular|rating|recent), `page`(1-base), `size` 파라미터를 지원하는 페이징 목록과 페이지 메타데이터를 반환해야 한다.
- **FR-011**: `GET /api/characters/{id}`는 캐릭터 소개, 평균 평점, 세션 수, 총 평가 수, 카테고리를 포함해 반환해야 한다.
- **FR-012**: 인증 사용자는 `GET /api/characters/favorites`로 즐겨찾기 목록을 최신순 페이징 결과로 조회할 수 있어야 한다.
- **FR-013**: 인증 사용자는 `POST /api/characters/{id}/favorite`와 `DELETE /api/characters/{id}/favorite`로 즐겨찾기를 추가·제거할 수 있어야 한다.
- **FR-014**: 캐릭터 목록과 상세 응답에는 로그인 사용자의 즐겨찾기 여부가 포함돼야 한다.

### 2.3 상담 세션 및 메시지
- **FR-020**: `POST /api/sessions`는 `characterId`를 입력받아 새 세션을 생성하고 기본 제목(`"새 대화"`)과 캐릭터 정보를 반환해야 한다.
- **FR-021**: `GET /api/sessions`는 `bookmarked`, `isClosed`, `page`(0-base), `size` 필터를 지원하고, 캐릭터 메타 정보까지 포함한 페이지 응답을 반환해야 한다.
- **FR-022**: `DELETE /api/sessions/{sessionId}`는 세션을 종료하고 `closedAt`을 기록해야 한다.
- **FR-023**: `PATCH /api/sessions/{sessionId}/bookmark`는 북마크 상태를 토글하고 현재 상태를 반환해야 한다.
- **FR-024**: `PATCH /api/sessions/{sessionId}/title`은 1~15자 제목으로 업데이트해야 한다.
- **FR-025**: `GET /api/sessions/{sessionId}/messages`는 메시지를 생성 시간 오름차순으로 페이징하여 반환해야 한다.
- **FR-026**: `POST /api/sessions/{sessionId}/messages`는 사용자 메시지를 저장하고 Spring AI ChatClient(OpenRouter)를 호출해 AI 응답을 생성·저장한 뒤 결과를 반환해야 한다. AI 호출 실패 시에는 정책 안내 문구를 반환해야 한다.
- **FR-027**: `POST /api/sessions/{sessionId}/rate`는 종료된 세션에 대해 1~10 정수 평점과 500자 이하 피드백을 저장해야 한다.
- **FR-028**: `POST /api/sessions/{sessionId}/messages/{messageId}/report`는 Reason 코드와 상세 설명을 받아 중복 신고를 방지하며 신고 내역을 저장해야 한다.

### 2.4 데이터 초기화 및 운영 지원
- **FR-030**: `InitDataConfig`는 애플리케이션 초기 실행 시 40여 개 캐릭터와 테스트 계정을 시드하고 기존 데이터가 있으면 건너뛴다.
- **FR-031**: `.env` 혹은 환경 변수로 `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, `JWT_SECRET`, `REDIS_URL`, OAuth 클라이언트 키를 설정할 수 있어야 한다.
- **FR-032**: `OAuthTokenCacheService`는 Google/Kakao 토큰 검증 결과를 Redis에 캐시해 재시도 시 외부 호출을 최소화해야 한다.
- **FR-033**: `RefreshTokenService`는 사용자별 Refresh 토큰 리스트를 Redis Set으로 관리하고 탈퇴 시 모두 폐기해야 한다.

### 2.5 모바일 클라이언트 요구
- **FR-040**: Expo 앱은 Bearer Access 토큰으로 API를 호출하고 401 응답 시 Refresh 플로우를 자동 수행해야 한다.
- **FR-041**: 클라이언트는 세션 목록을 진행/종료/북마크 탭으로 구분하고 제목·북마크 변경 시 캐시를 즉시 갱신해야 한다.
- **FR-042**: 메시지 신고 UI는 Reason 코드를 Enum 셀렉터로 제공하며 사용자 입력을 2000자 이하로 제한해야 한다.
## 3. 비기능 요구사항

### 3.1 성능
- **NFR-001**: API 응답은 정상 시 300ms 이내, AI 응답은 60초 타임아웃 내에 완료돼야 한다.
- **NFR-002**: 캐릭터 목록·상세, 세션 목록·메시지 조회는 Redis 캐시를 활용하고 캐시 미스 시에도 1초 내 응답해야 한다.
- **NFR-003**: 페이지 크기(`size`)는 1~100 범위로 제한하며, 기본값은 20이다.

### 3.2 보안
- **NFR-010**: 모든 민감 설정은 환경 변수로 주입하며 저장소에는 커밋하지 않는다.
- **NFR-011**: JWT 서명키는 256비트 이상이어야 하며 Access/Refresh 만료는 각각 24시간/30일로 유지한다.
- **NFR-012**: `JwtAuthenticationFilter`는 `/api/auth/**`, `/swagger-ui/**`, `/api/characters` GET을 제외한 모든 `/api/**` 요청에 인증을 강제해야 한다.
- **NFR-013**: 메시지 신고 엔드포인트는 신고자와 메시지의 세션 소유 여부를 검증해야 한다.
### 3.3 안정성 및 관측성
- **NFR-020**: Spring Boot Actuator `/actuator/health`는 운영 헬스체크에 사용된다.
- **NFR-021**: OpenRouter AI 호출 실패는 WARN 이상 로그와 함께 사용자가 재시도할 수 있는 메시지를 반환해야 한다.
- **NFR-022**: OAuth·Redis 예외는 Sentry 등 외부 APM 연동을 고려할 수 있도록 structured logging을 유지한다.

### 3.4 유지보수성
- **NFR-030**: 코드 스타일은 ktlint 1.0과 detekt 1.23 규칙을 따른다.
- **NFR-031**: 모든 주요 도메인은 단위 테스트 혹은 MockMvc 통합 테스트를 보유하며 `./gradlew test`로 실행 가능해야 한다.
- **NFR-032**: GitHub Actions CI는 `ktlintCheck`, `detekt`, `test`를 병렬 실행한다.
## 4. 시스템 제약사항
- Kotlin 1.9.25, Spring Boot 3.5.4, Java 21
- Gradle 8.14.3 빌드 스크립트
- Spring AI 1.0.0-M6 (`ChatClient`) + OpenRouter API
- 데이터베이스: PostgreSQL 15 (운영), H2 (테스트)
- 캐시: Redis (옵션, 미설정 시 기본 메모리 캐시)
- 모바일: Expo SDK 54, React Native 0.74, React 19 (클라이언트 레포지토리)
- 테스트 프레임워크: JUnit5, MockK, SpringMockK
## 5. 인터페이스 요구사항
- **외부**: Google OAuth, Kakao OAuth, OpenRouter Chat Completions API
- **내부**: 모든 REST API는 `/api` 프리픽스, JSON Req/Res, `Authorization: Bearer {token}` 헤더 사용
- **문서화**: SpringDoc Swagger UI (`/swagger-ui/index.html`)와 `docs/api-specification.yaml` OpenAPI 3.0 문서 유지
## 6. 데이터 요구사항
- **users**: `email`, `nickname`, `auth_provider(GOOGLE|KAKAO)`, `provider_id`, `profile_image_url`, `is_active`, `last_login_at`, `created_at`
- **characters**: `name`, `title`, `description`, `base_prompt`, `avatar_url`, `categories`, `is_active`, `created_at`
- **chat_sessions**: `user_id`, `character_id`, `title(<=100)`, `is_bookmarked`, `last_message_at`, `closed_at`, `created_at`
- **messages**: `session_id`, `sender_type(USER|AI)`, `content(TEXT)`, `created_at`
- **favorite_characters**: `user_id + character_id` 유니크, 생성 시각
- **character_ratings**: `user_id`, `character_id`, `session_id`(유니크), `rating(1~10)`, `review(TEXT, 500 자)`
- **message_reports**: `message_id`, `reporter_user_id`, `reason_code(HARASSMENT|SELF_HARM|HATE_SPEECH|MISINFORMATION|SPAM|OTHER)`, `detail(TEXT)`, `created_at`

모든 테이블은 `BaseEntity`를 상속해 `id`, `created_at`, `updated_at`을 공통 관리한다.
## 7. AI 상담 로직
- `ChatSessionService.sendMessage`는 Spring AI `ChatClient`로 JSON 응답을 요청하고, 실패 시 정책 안내 문구를 저장한다.
- 시스템 프롬프트는 캐릭터별 `base_prompt` + 안전 가이드 + 직전 대화 요약으로 구성된다.
- 첫 AI 응답이 성공하면 15자 이내 제목을 생성해 세션 타이틀을 갱신한다.
- AI 응답은 JSON을 파싱해 `content`와 선택적 `title`만 사용하며, Markdown 코드블록을 제거한다.
- AI 호출 실패 시 fallback 메시지를 저장하고 세션 타임스탬프를 유지한다.
## 8. 품질 & 테스트
- `./gradlew check-all`: ktlint + detekt + test 통합 실행
- `./gradlew ktlintFormat`: 스타일 자동 정렬
- `./gradlew detekt`: 정적 분석 보고서 생성
- `./gradlew test`: 약 20여 개의 단위·통합 테스트를 실행 (MockMvc 컨트롤러 테스트 포함)
- Git hooks (`installGitHooks`)는 커밋 전 ktlint 검사를 수행한다.
## 9. 프로젝트 현황
- **완료**: Google/Kakao OAuth 로그인, JWT 인증/Refresh 토큰 회전, 캐릭터 목록·상세·즐겨찾기, 세션/메시지 파이프라인, Redis 캐시, 메시지 신고, Spring AI 연동
- **진행 중**: Expo 앱 UX 정교화, 세션 종료 후 리뷰 피드백 UI 개선, Redis 미사용 환경을 위한 캐시 graceful fallback
- **예정**: 세션 검색/요약, 캐릭터 추천 로직, 푸시 알림 및 유료 구독 플랜, OpenRouter 대체 모델 실험
## 10. 위험 관리
- **기술**: OpenRouter API SLA, OAuth 제공자 정책 변경, Redis 장애로 인한 캐시 리셋
- **비즈니스**: 상담 품질 관리, 개인정보 처리 및 콘텐츠 신고 대응, 트래픽 증가에 따른 인프라 비용
- **완화 전략**: AI 호출 실패 시 안내 메시지 제공, OAuth 검증 실패 로그 모니터링, 환경 변수로 모델 스위칭, Redis 비가용 시 캐시 미사용 모드 운영
