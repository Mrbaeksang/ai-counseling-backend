<div align="center">

# 🧠 AI 철학 상담 앱

### **AI 철학자들과 함께하는 맞춤형 상담 서비스**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen?style=for-the-badge&logo=spring)](https://spring.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Tests](https://img.shields.io/badge/Tests-103%20Passed-success?style=for-the-badge)](https://github.com/Mrbaeksang/ai-counseling-app)
[![License](https://img.shields.io/badge/License-Private-red?style=for-the-badge)]()

[**📚 API 문서**](./docs/api-specification.yaml) • 
[**🏗️ 아키텍처**](./docs/system-architecture.md) • 
[**📊 데이터베이스**](./docs/erd-diagram.md) • 
[**📋 요구사항**](./docs/SRS.md)

</div>

---

## ✨ 프로젝트 소개

**AI 철학 상담 앱**은 역사적 철학자들의 사상과 상담 기법을 AI로 구현한 혁신적인 상담 서비스입니다. 
OpenRouter API를 통해 각 철학자의 고유한 상담 스타일을 재현하며, 5단계 상담 프로세스를 통해 체계적인 심리 상담을 제공합니다.

### 🎯 핵심 특징

- 🤖 **30+ AI 철학자/상담사** - 소크라테스부터 현대 심리학자까지
- 🔄 **5단계 상담 프로세스** - AI가 자율적으로 상담 단계 전환
- 🔐 **OAuth 2.0 인증** - Google, Kakao, Naver 소셜 로그인
- 📱 **RESTful API** - 모바일 앱 연동 준비 완료
- ✅ **100% 테스트 커버리지** - 103개 테스트 케이스 통과

---

## 🚀 Quick Start

### Prerequisites

- JDK 21+
- Gradle 8.5+
- PostgreSQL 14+ (Production)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Mrbaeksang/ai-counseling-app.git
cd ai-counseling-app
```

2. **Set environment variables**
```bash
# .env 파일 생성
OPENROUTER_API_KEY=your_api_key_here
JWT_SECRET=your_jwt_secret_here
DB_URL=jdbc:postgresql://localhost:5432/aicounseling
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

3. **Run the application**
```bash
./gradlew bootRun
```

4. **Access Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

---

## 🏗️ Architecture

### Tech Stack

<table>
<tr>
<td align="center" width="50%">

**Backend**
- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.4
- **Architecture**: DDD (Domain-Driven Design)
- **Database**: PostgreSQL + H2
- **ORM**: JPA + JDSL 3.5.5
- **API**: RESTful + OpenAPI 3.0
- **Testing**: JUnit 5 + MockK

</td>
<td align="center" width="50%">

**Infrastructure**
- **Authentication**: OAuth 2.0 + JWT
- **AI Integration**: OpenRouter API
- **Model**: meta-llama/llama-3.2-3b
- **Async**: WebFlux + Coroutines
- **CI/CD**: GitHub Actions
- **Deployment**: Railway / Docker

</td>
</tr>
</table>

### Project Structure

```
src/main/kotlin/com/aicounseling/app/
├── 📁 domain/                    # 비즈니스 도메인
│   ├── 👤 user/                 # 사용자 관리
│   │   ├── entity/
│   │   ├── service/
│   │   ├── repository/
│   │   └── controller/
│   ├── 🧑‍🏫 counselor/           # 상담사 관리
│   │   ├── entity/
│   │   ├── service/
│   │   ├── repository/
│   │   └── controller/
│   └── 💬 session/              # 상담 세션
│       ├── entity/
│       ├── service/
│       ├── repository/
│       └── controller/
└── 🌐 global/                   # 공통 관심사
    ├── auth/                    # OAuth 인증
    ├── config/                  # Spring 설정
    ├── security/                # JWT 보안
    ├── openrouter/              # AI API 통합
    └── exception/               # 전역 예외 처리
```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| 📘 [API Specification](./docs/api-specification.yaml) | OpenAPI 3.0 스펙 문서 |
| 🏛️ [System Architecture](./docs/system-architecture.md) | 시스템 아키텍처 설계 |
| 📊 [ERD Diagram](./docs/erd-diagram.md) | 데이터베이스 설계 |
| 📋 [Requirements (SRS)](./docs/SRS.md) | 소프트웨어 요구사항 명세 |
| 🎭 [Use Case Diagram](./docs/use-case-diagram.md) | 유스케이스 다이어그램 |

---

## 🔥 Features

### ✅ 구현 완료 (Phase 1 - MVP, 2025년 9월)

#### 🔐 인증 시스템
- [x] OAuth 2.0 소셜 로그인 (Google, Kakao, Naver)
- [x] JWT 토큰 기반 인증 (Access + Refresh Token)
- [x] 사용자 프로필 관리
- [x] 회원 탈퇴

#### 💬 상담 시스템
- [x] 30+ AI 철학자/상담사 구현
- [x] 5단계 자동 상담 프로세스
  - `ENGAGEMENT` - 관계 형성
  - `EXPLORATION` - 문제 탐색
  - `INSIGHT` - 통찰 유도
  - `ACTION` - 행동 계획
  - `CLOSING` - 마무리
- [x] 실시간 AI 응답 생성
- [x] 대화 히스토리 관리

#### 📊 세션 관리
- [x] 세션 생성/종료
- [x] 세션 북마크
- [x] 세션 제목 자동 생성 및 수정
- [x] 세션별 평가 시스템 (1-10점)

#### 🌟 상담사 기능
- [x] 상담사 목록 조회
- [x] 상담사 상세 정보
- [x] 즐겨찾기 관리
- [x] 상담사별 통계

### 🚧 개발 예정 (Phase 2)

- [ ] Android 앱 개발
- [ ] 세션 요약 기능
- [ ] 대화 내용 검색
- [ ] 상담 통계 대시보드

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "*.UserServiceTest"

# Generate test report
./gradlew jacocoTestReport
```

### Test Results

<div align="center">

| Category | Count | Status |
|----------|-------|--------|
| **Total Tests** | 103 | ✅ All Passed |
| **Unit Tests** | 78 | ✅ Passed |
| **Integration Tests** | 25 | ✅ Passed |
| **Test Coverage** | 100% | 🎯 Complete |

</div>

---

## 📡 API Endpoints

### Authentication
```http
POST   /api/auth/login/google     # Google OAuth 로그인
POST   /api/auth/login/kakao      # Kakao OAuth 로그인
POST   /api/auth/login/naver      # Naver OAuth 로그인
POST   /api/auth/refresh          # 토큰 갱신
```

### User Management
```http
GET    /api/users/me              # 내 정보 조회
PATCH  /api/users/nickname        # 닉네임 변경
DELETE /api/users/me              # 회원 탈퇴
```

### Counselor
```http
GET    /api/counselors            # 상담사 목록
GET    /api/counselors/{id}       # 상담사 상세
GET    /api/counselors/favorites  # 즐겨찾기 목록
POST   /api/counselors/{id}/favorite    # 즐겨찾기 추가
DELETE /api/counselors/{id}/favorite    # 즐겨찾기 제거
```

### Session & Chat
```http
GET    /api/sessions              # 세션 목록
POST   /api/sessions              # 새 세션 시작
POST   /api/sessions/{id}/messages      # 메시지 전송
GET    /api/sessions/{id}/messages      # 메시지 조회
POST   /api/sessions/{id}/close         # 세션 종료
POST   /api/sessions/{id}/rating        # 세션 평가
POST   /api/sessions/{id}/bookmark      # 북마크 토글
PATCH  /api/sessions/{id}/title         # 제목 변경
```

---

## 🚀 Deployment

### Development
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Production
```bash
docker build -t ai-counseling-app .
docker run -p 8080:8080 --env-file .env ai-counseling-app
```

---

## 🤝 Contributing

기여는 언제나 환영합니다! 다음 절차를 따라주세요:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feat/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feat/AmazingFeature`)
5. Open a Pull Request

### Commit Convention

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
refactor: 코드 리팩토링
test: 테스트 추가
style: 코드 포맷팅
```

---

## 📄 License

Private Repository - 상업적 사용 금지

---

## 👨‍💻 Developer

<div align="center">

**백상현 (Sanghyeon Baek)**

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Mrbaeksang)
[![Email](https://img.shields.io/badge/Email-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:qortkdgus95@gmail.com)

</div>

---

<div align="center">

### 🌟 Star this repository if you find it helpful!

</div>