# 소프트웨어 요구사항 명세서 (SRS)

## 1. 개요

### 1.1 목적
AI Counseling App의 백엔드 및 모바일 클라이언트가 제공해야 하는 기능과 품질 목표를 정의한다. 본 문서는 개발, 테스트, 운영 단계에서 공통 참조 지점으로 활용한다.

### 1.2 범위
- 프로젝트명: AI Counseling App
- 플랫폼: Spring Boot 기반 REST API + Expo/React Native 클라이언트
- 주요 시나리오: 역사·심리 상담사 페르소나와의 1:1 대화, 세션 관리, 평가 및 즐겨찾기

### 1.3 용어 정의
- **상담사**: `counselors` 테이블에 등록된 AI 페르소나
- **세션**: `chat_sessions` 레코드로 표현되는 대화 단위
- **상담 단계**: ENGAGEMENT, EXPLORATION, INSIGHT, ACTION, CLOSING 다섯 단계
- **북마크**: 사용자가 다시 보고 싶은 세션에 표시한 즐겨찾기
- **OAuth 토큰 검증기**: Google/Kakao SDK 대신 호출하는 백엔드 전용 토큰 검증 서비스

## 2. 기능 요구사항

### 2.1 인증 및 사용자
- **FR-001**: 사용자는 Google OAuth 토큰으로 `/api/auth/login/google`에 로그인할 수 있어야 한다.
- **FR-002**: 사용자는 Kakao OAuth 토큰으로 `/api/auth/login/kakao`에 로그인할 수 있어야 한다.
- **FR-003**: 로그인 성공 시 백엔드는 Access/Refresh JWT를 발급하고 사용자 정보를 반환해야 한다.
- **FR-004**: `/api/auth/refresh`는 유효한 `refreshToken`으로 새로운 토큰 쌍을 발급해야 한다.
- **FR-005**: `/api/users/me`는 로그인 사용자의 프로필을 반환해야 한다.
- **FR-006**: `/api/users/nickname`은 닉네임을 2~20자 사이 문자열로 수정할 수 있어야 한다.
- **FR-007**: `/api/users/me` DELETE 요청은 계정과 연계 데이터(세션, 메시지, 평가 등)를 삭제해야 한다.

### 2.2 상담사
- **FR-010**: `/api/counselors`는 `sort`(popular|rating|recent), `page`(1-base), `size` 파라미터를 지원하는 페이징 목록을 반환해야 한다.
- **FR-011**: `/api/counselors/{id}`는 상담사의 소개, 평균 평점, 총 세션 수, 카테고리를 포함해야 한다.
- **FR-012**: 인증 사용자는 `/api/counselors/{id}/favorite` POST/DELETE로 즐겨찾기 추가/제거를 수행할 수 있어야 한다.
- **FR-013**: `/api/counselors/favorites`는 로그인 사용자의 즐겨찾기 목록을 최신순으로 반환해야 한다.

### 2.3 상담 세션 및 메시지
- **FR-020**: `/api/sessions` POST는 `counselorId`를 입력받아 새 세션을 생성하고 기본 제목("새 상담")을 반환해야 한다.
- **FR-021**: `/api/sessions` GET은 `bookmarked`, `isClosed`, `page`(0-base), `size` 파라미터 기반으로 세션 목록과 페이지 정보를 반환해야 한다.
- **FR-022**: `/api/sessions/{id}/messages` GET은 생성 시간 오름차순으로 페이징된 메시지를 반환해야 한다.
- **FR-023**: `/api/sessions/{id}/messages` POST는 사용자 메시지를 저장하고 OpenRouter 모델로 AI 응답을 생성하여 반환해야 한다. 첫 응답에는 세션 제목을 포함한다.
- **FR-024**: `/api/sessions/{id}/bookmark` PATCH는 북마크 상태를 토글하며 결과 boolean을 반환해야 한다.
- **FR-025**: `/api/sessions/{id}/title` PATCH는 1~15자 제목으로 업데이트해야 한다.
- **FR-026**: `/api/sessions/{id}` DELETE는 진행 중인 세션을 종료하고 `closedAt`을 기록해야 한다.
- **FR-027**: `/api/sessions/{id}/rate` POST는 종료된 세션에 대해 1~10 정수 평점과 500자 이하 피드백을 저장해야 한다.

### 2.4 데이터 초기화 및 운영 지원
- **FR-030**: `InitDataConfig`는 초기 실행 시 40여 명의 상담사 및 테스트 계정을 로드해야 하며 이미 데이터가 존재하면 건너뛴다.
- **FR-031**: 관리자는 `.env`에 API 키와 OAuth 클라이언트 정보를 설정해 환경별 배포를 제어할 수 있어야 한다.

### 2.5 모바일 클라이언트 요구
- **FR-040**: Expo 앱은 React Query 기반으로 위 API를 호출해야 하며, Access Token이 만료되면 Refresh 플로우를 자동 수행해야 한다.
- **FR-041**: 앱은 세션 목록을 탭(진행/종료/북마크)으로 구분해 보여주고 제목/북마크 상태 변경 시 캐시를 갱신해야 한다.

## 3. 비기능 요구사항

### 3.1 성능
- **NFR-001**: API 응답은 정상 시 2초 이내, AI 응답은 60초 설정된 타임아웃 내에 완료되어야 한다.
- **NFR-002**: OpenRouter 호출 실패 시 최대 3회 지수 백오프 재시도를 수행하고 실패 원인을 로깅해야 한다.
- **NFR-003**: 세션 목록과 메시지 조회는 PageRequest 기반으로 20건씩 반환하고, `size`는 최소 1, 최대 100으로 제한한다.

### 3.2 보안
- **NFR-010**: 모든 민감 설정은 `.env` 또는 Railway 환경변수로 관리한다.
- **NFR-011**: JWT 서명키는 최소 256비트 이상이며 Access/Refresh 만료는 각각 24시간/7일로 유지한다.
- **NFR-012**: OAuth 토큰 검증은 Google/Kakao 벤더 API 혹은 SDK 래퍼(`GoogleTokenVerifier`, `KakaoTokenVerifier`)를 통해 수행한다.

### 3.3 안정성 및 관측성
- **NFR-020**: Spring Boot Actuator `/actuator/health` 엔드포인트는 운영 헬스체크에 사용된다.
- **NFR-021**: OpenRouter 및 OAuth 통신 오류는 WARN 이상 레벨로 로그 남기고 사용자에게 재시도 메시지를 제공한다.

### 3.4 유지보수
- **NFR-030**: 코드 스타일은 ktlint와 detekt 규칙을 따른다.
- **NFR-031**: 테스트는 JUnit5 + MockK로 작성하고 `./gradlew test` 명령으로 실행한다.

## 4. 시스템 제약사항
- Kotlin 1.9.25, Spring Boot 3.5.4, Java 21
- Gradle 8.14.3 빌드, ktlint/detekt 플러그인 사용
- 데이터베이스: PostgreSQL 15 (운영), H2 in-memory (테스트)
- 모바일: Expo SDK 54, React Native 0.81, React 19
- AI 모델 기본값: `openrouter/sonoma-sky-alpha` (`application.yml`), 미설정 시 `meta-llama/llama-3.2-3b-instruct`

## 5. 인터페이스 요구사항
- **외부**: Google OAuth, Kakao OAuth, OpenRouter Chat Completions API
- **내부**: 모든 API는 `/api` 프리픽스, JSON Body/Response, `Authorization: Bearer {token}` 헤더 사용
- **문서화**: SpringDoc Swagger UI (`/swagger-ui/index.html`) 자동 노출

## 6. 데이터 요구사항
- **users**: email, nickname, auth_provider(GOOGLE/KAKAO), provider_id, profile_image_url, is_active, last_login_at, available_sessions, daily_ads_watched, ads_reset_date
- **counselors**: name, title, description, base_prompt, avatar_url, categories(csv), is_active
- **chat_sessions**: user_id, counselor_id, title(<=100), is_bookmarked, last_message_at, closed_at
- **messages**: session_id, sender_type(USER/AI), content(TEXT), phase(ENGAGEMENT~CLOSING)
- **favorite_counselors**: user_id + counselor_id unique
- **counselor_ratings**: session_id unique, rating(1~10), review(TEXT)

데이터는 사용자 탈퇴 시 삭제되며, 세션/메시지는 사용자의 결정 전까지 보관한다.

## 7. AI 상담 로직
- 첫 메시지 응답 시 제목을 생성해 `chat_sessions.title`에 반영한다.
- `AppConstants.Session.PROFESSIONAL_COUNSELING_GUIDE` 가이드라인과 5단계 규칙을 사용한다.
- 이전 단계로 회귀하지 않으며 단계별 메시지 수 임계치를 통해 자연스러운 전환을 유도한다.

## 8. 품질 및 테스트
- `./gradlew check-all`로 ktlint, detekt, 단위/통합 테스트를 통합 실행한다.
- 2025-09-20 기준 95개의 테스트가 실행되며, Google/Kakao 기존 사용자 로그인 시나리오를 포함한 3개 테스트가 신규 트랜잭션 설정 변경 이후 재검증이 필요하다.

## 9. 프로젝트 현황 (2025-09)
- 완료: OAuth 로그인(Google/Kakao), JWT 인증, 상담사/세션 API, AI 연동, Expo 앱 기본 UX
- 진행 중: Android 심사 패키징, 세션 검색·요약 기능, 베타 사용자 피드백 수집
- 예정: Redis 캐시 도입, 알림/Push 채널, 유료 구독 플랜

## 10. 위험 관리
- **기술**: OpenRouter API SLA, OAuth 제공자 정책 변경, 장시간 AI 응답으로 인한 타임아웃
- **비즈니스**: 상담 품질 검증, 개인정보 처리 규정, 트래픽 증가 대비 인프라 비용
- **완화 전략**: 장애 시 AI 응답 FALLBACK 메시지 제공, OAuth 토큰 검증기의 실패 로그 모니터링, 환경변수 기반 모델 스위칭
