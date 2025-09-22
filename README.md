<div align="center">

# 🧠 AI 철학 상담 앱 (Personal Project)

### **AI Persona와의 1:1 상담을 위한 Kotlin/Spring 백엔드**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Tests](https://img.shields.io/badge/Tests-95%20cases-blue?style=for-the-badge)]()
[![Status](https://img.shields.io/badge/Status-Personal%20Project-blueviolet?style=for-the-badge)]()

[**📋 요구사항 (SRS)**](docs/SRS.md) •
[**🏗️ 아키텍처**](docs/system-architecture.md) •
[**📊 ERD**](docs/erd-diagram.md) •
[**🗨️ 유스케이스**](docs/use-case-diagram.md) •
[**🧾 API 스펙**](docs/api-specification.yaml)

</div>

---

## ✨ 프로젝트 소개

이 레포는 제가 설계·구현하고 있는 개인용 상담 서비스 실험입니다. 역사·심리학 기반 상담사 페르소나를 AI로 재현하고, 5단계 상담 프로세스를 통해 사용자가 지속적인 대화를 이어갈 수 있도록 돕습니다.

### 핵심 특징
- 🤖 **40+ AI 상담사 페르소나** – `InitDataConfig`를 통해 시드되고 DDD 계층으로 분리된 도메인 로직이 응답을 조율합니다.
- 🔄 **5단계 상담 프로세스 엔진** – ENGAGEMENT → EXPLORATION → INSIGHT → ACTION → CLOSING 자동 전환과 제목 생성 로직 내장.
- 🔐 **이중 토큰 인증** – Google/Kakao OAuth 검증 후 Access/Refresh JWT 발급, `RsData` 포맷으로 일관 응답.
- 💬 **세션·메시지 파이프라인** – 자동 제목, 북마크, 종료, 평가, 메시지 페이징을 하나의 `ChatSessionService`에서 orchestration.
- 📊 **Kotlin JDSL 통계 쿼리** – 상담사 목록/상세에서 평균 평점, 세션 수를 실시간 집계.

문서는 `SRS → system-architecture → erd/use-case → api-specification` 순으로 읽으면 흐름이 자연스럽습니다.

---

## 🗂️ 리포지토리 구조 (Backend)
```text
backend/
├── docs/                  # 요구사항·아키텍처·ERD·API 문서
├── src/
│   ├── main/
│   │   ├── kotlin/com/aicounseling/app/
│   │   │   ├── domain/      # user, counselor, session 도메인 계층
│   │   │   └── global/      # auth, security, openrouter, rsData 등 공통 계층
│   │   └── resources/       # application.yml, 프로필 설정, 초기 데이터 템플릿
│   └── test/                # MockMvc + Spring Boot 통합/단위 테스트
├── build.gradle.kts        # Gradle 8.14.3 빌드 스크립트
└── README.md
```

---

## 🛠️ 기술 스택 (Backend)
| 범주 | 사용 기술 |
|------|-----------|
| Core Runtime | Kotlin 1.9.25 · Spring Boot 3.5.4 · Java 21 · Gradle 8.14.3 |
| Web & API | Spring MVC/MockMvc · Spring Security · WebFlux(WebClient) · SpringDoc OpenAPI 2.7 |
| Data Layer | Spring Data JPA · Kotlin JDSL 3.5.5 · PostgreSQL 15 · H2 (test) |
| AI Integration | OpenRouter Chat Completions · Coroutines/Reactive WebClient |
| Code Quality | ktlint 12 · detekt 1.23 · JUnit5 · MockK · SpringMockK |
| Operations | Actuator · GitHub Actions · Railway (prod) |

---

## 🚀 빠른 시작
### Backend
```bash
# 필수: JDK 21+, Git, (옵션) PostgreSQL 15
cd backend
cp .env.example .env
# OPENROUTER_API_KEY, JWT_SECRET, DB_URL/USER/PASSWORD, GOOGLE/KAKAO 클라이언트 키 입력
# (선택) REDIS_URL=redis://user:pass@host:port 형식으로 설정 시 Redis 캐시 활성화
./gradlew bootRun
# Swagger UI  : http://localhost:8080/swagger-ui/index.html
# Actuator    : http://localhost:8080/actuator/health
```
- 기본 프로파일은 `dev`; 운영 배포 시 `SPRING_PROFILES_ACTIVE=prod`로 ResponseAspect를 활성화합니다.

### Frontend
```bash
# 필수: Node.js 18 LTS 이상, npm, Expo CLI
cd frontend
cp .env.example .env
# API_BASE_URL, GOOGLE/KAKAO CLIENT ID 설정 (NAVER 항목은 현재 미사용)
npm install
npm run start    # 또는 npx expo start
```
- Expo DevTools에서 `i`(iOS), `a`(Android), `w`(Web)로 실행하거나 Expo Go 앱으로 QR 스캔합니다.

---

## ✅ 구현 현황
### 인증 & 사용자
- [x] Google / Kakao OAuth 로그인 → JWT 발급 (`/api/auth/login/{provider}`)
- [x] Refresh 토큰 재발급 (`/api/auth/refresh`)
- [x] 프로필 조회 (`/api/users/me`), 닉네임 변경, 회원 탈퇴

### 상담사 & 세션
- [x] 상담사 목록/상세 (정렬: popular/rating/recent), 즐겨찾기 관리
- [x] 세션 생성 → 메시지 전송 → AI 응답 → 북마크/제목 수정 → 종료 흐름
- [x] 세션 평가 (1~10점, 500자 피드백)
- [x] 메시지 페이징 조회, 상담 단계 추적, 자동 제목 생성

### 플랫폼 & 통합
- [x] ResponseAspect 기반 `RsData` ↔ HTTP Status 매핑 (prod 프로필)
- [x] OpenRouter WebClient 60초 타임아웃 + 3회 재시도 및 오류 로깅
- [x] Actuator health 체크, Gradle `check-all` 파이프라인, GitHub Actions CI

> 테스트는 현재 95개가 실행되며, OAuth 통합 시나리오는 트랜잭션 설정 변경 이후 재검증이 필요합니다. `./gradlew test --tests "*LoginApiTest"`로 확인하세요.

---

## 🔬 품질 & 자동화
| 명령 | 설명 |
|------|------|
| `./gradlew ktlintCheck` / `ktlintFormat` | Kotlin 스타일 검사 / 자동 정렬 |
| `./gradlew detekt` | 정적 분석 |
| `./gradlew test` | 백엔드 단위·통합 테스트 |
| `./gradlew check-all` | ktlint + detekt + test 통합 실행 |
| `./gradlew jacocoTestReport` | (선택) 코드 커버리지 리포트 생성 |

GitHub Actions 워크플로(`.github/workflows/ci.yml`, `pr-check.yml`)에서 위 명령을 자동으로 실행합니다.

---

## 📚 문서 흐름
| 문서 | 설명 |
|------|------|
| [`docs/SRS.md`](docs/SRS.md) | 기능·비기능 요구사항, 용어 정의, 위험 관리 |
| [`docs/system-architecture.md`](docs/system-architecture.md) | 시스템 구성, 시퀀스 다이어그램, 배포 전략 |
| [`docs/erd-diagram.md`](docs/erd-diagram.md) | ERD, 테이블 속성, 인덱스 전략 |
| [`docs/use-case-diagram.md`](docs/use-case-diagram.md) | 유스케이스 다이어그램, 진행 상태 |
| [`docs/api-specification.yaml`](docs/api-specification.yaml) | OpenAPI 3.0 명세 (RsData 스키마 포함) |

> 위 순서대로 읽으면 설계 의도와 구현 범위를 빠르게 파악할 수 있습니다.

---

## 🔌 주요 API 요약
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/login/{google,kakao}` | OAuth 토큰 검증 + JWT 발급 |
| POST | `/api/auth/refresh` | Refresh 토큰으로 Access/Refresh 재발급 |
| GET | `/api/users/me` | 내 프로필 조회 |
| PATCH | `/api/users/nickname` | 닉네임 변경 |
| GET | `/api/counselors` | 상담사 목록 (정렬/페이징) |
| GET | `/api/counselors/{id}` | 상담사 상세 |
| POST | `/api/counselors/{id}/favorite` | 즐겨찾기 추가 (DELETE 제거) |
| GET | `/api/sessions` | 세션 목록 (bookmarked / isClosed 필터) |
| POST | `/api/sessions/{id}/messages` | 메시지 전송 + AI 응답 |
| PATCH | `/api/sessions/{id}/bookmark` | 세션 북마크 토글 |
| PATCH | `/api/sessions/{id}/title` | 세션 제목 수정 |
| POST | `/api/sessions/{id}/rate` | 세션 평가 |

자세한 요청/응답 스키마와 코드 예시는 Swagger UI 혹은 OpenAPI 파일에서 확인할 수 있습니다.

---

## 📦 배포 메모
- **Backend**: Railway (PostgreSQL) + OpenRouter API 키 환경 변수, `prod` 프로필 사용.
- **Frontend**: Expo EAS Build, Google/Kakao 네이티브 키는 Expo Config Plugins로 주입.
- **ResponseAspect**: 운영 환경에서 `RsData.resultCode`에 따라 HTTP Status를 재설정합니다 (`S-204` → 204 등).

---

<div align="center">

### 🙌 프로젝트 관련 제안이나 질문은 이슈로 남겨주세요.

</div>
