# 유스케이스 다이어그램

## AI 상담 시스템 Use Case Diagram
```mermaid
graph TB
    classDef actor fill:#1976D2,stroke:#0D47A1,stroke-width:2px,color:#FFFFFF
    classDef usecase fill:#4CAF50,stroke:#2E7D32,stroke-width:2px,color:#FFFFFF
    classDef system fill:#9C27B0,stroke:#4A148C,stroke-width:3px,color:#FFFFFF

    User[👤 사용자]:::actor
    OAuth[🔐 OAuth Providers\n(Google · Kakao)]:::actor
    AI[🤖 OpenRouter AI]:::actor

    subgraph System[" AI 상담 시스템 (Spring Boot + Kotlin) "]
        UC1[OAuth 로그인<br/>Google · Kakao]:::usecase
        UC2[JWT 토큰 발급]:::usecase
        UC3[토큰 갱신]:::usecase
        UC4[프로필 조회]:::usecase
        UC5[닉네임 변경]:::usecase
        UC6[회원 탈퇴]:::usecase

        UC7[상담사 목록]:::usecase
        UC8[상담사 상세]:::usecase
        UC9[즐겨찾기 관리]:::usecase
        UC10[즐겨찾기 목록]:::usecase

        UC11[새 세션 시작]:::usecase
        UC12[세션 목록 조회]:::usecase
        UC13[세션 종료]:::usecase
        UC14[세션 북마크 토글]:::usecase
        UC15[세션 제목 수정]:::usecase

        UC16[메시지 전송]:::usecase
        UC17[AI 응답 생성]:::usecase
        UC18[메시지 목록 조회]:::usecase
        UC19[상담 단계 관리]:::usecase

        UC20[세션 평가]:::usecase
        UC21[피드백 작성]:::usecase
    end

    User --> UC1
    User --> UC3
    User --> UC4
    User --> UC5
    User --> UC6
    User --> UC7
    User --> UC8
    User --> UC9
    User --> UC10
    User --> UC11
    User --> UC12
    User --> UC13
    User --> UC14
    User --> UC15
    User --> UC16
    User --> UC18
    User --> UC20
    User --> UC21

    OAuth --> UC1
    OAuth --> UC2

    AI --> UC17
    AI --> UC19

    UC1 -.include.-> UC2
    UC11 -.include.-> UC7
    UC16 -.include.-> UC17
    UC17 -.include.-> UC19
    UC13 -.include.-> UC20

    UC7 -.extend.-> UC8
    UC12 -.extend.-> UC14
    UC12 -.extend.-> UC15
    UC20 -.extend.-> UC21
```

## 유스케이스 상세

### 🔐 인증 시스템
| Use Case | 설명 | 상태 |
|----------|------|------|
| OAuth 로그인 | Google, Kakao 소셜 로그인 | ✅ 완료 |
| JWT 토큰 발급 | Access & Refresh 토큰 발급 | ✅ 완료 |
| 토큰 갱신 | Refresh 토큰으로 재발급 | ✅ 완료 |
| 프로필 조회 | `/api/users/me` | ✅ 완료 |
| 닉네임 변경 | `/api/users/nickname` | ✅ 완료 |
| 회원 탈퇴 | `/api/users/me` DELETE | ✅ 완료 |

### 🧑‍🏫 상담사 관리
| Use Case | 설명 | 상태 |
|----------|------|------|
| 상담사 목록 | 정렬/페이징 지원 (`sort`, `page`, `size`) | ✅ 완료 |
| 상담사 상세 | 평균 평점, 통계 포함 | ✅ 완료 |
| 즐겨찾기 관리 | POST/DELETE `/favorite` | ✅ 완료 |
| 즐겨찾기 목록 | `/api/counselors/favorites` | ✅ 완료 |

### 💬 상담 세션
| Use Case | 설명 | 상태 |
|----------|------|------|
| 세션 시작 | `/api/sessions` POST | ✅ 완료 |
| 세션 목록 | 북마크·종료 상태 필터 | ✅ 완료 |
| 세션 종료 | `/api/sessions/{id}` DELETE | ✅ 완료 |
| 세션 북마크 | `/api/sessions/{id}/bookmark` PATCH | ✅ 완료 |
| 제목 수정 | `/api/sessions/{id}/title` PATCH | ✅ 완료 |

### 🗨️ 메시지 및 AI
| Use Case | 설명 | 상태 |
|----------|------|------|
| 메시지 전송 | 사용자 메시지 저장 | ✅ 완료 |
| AI 응답 생성 | OpenRouter 모델 호출, 상담 단계 판단 | ✅ 완료 |
| 메시지 목록 | 생성 시간 ASC 페이징 | ✅ 완료 |
| 상담 단계 관리 | 5단계 자동 전환 | ✅ 완료 |

### ⭐ 세션 평가
| Use Case | 설명 | 상태 |
|----------|------|------|
| 세션 평가 | 1~10 정수 평점 | ✅ 완료 |
| 피드백 작성 | 500자 이하 텍스트 | ✅ 완료 |

## 향후 확장 유스케이스
| Use Case | 설명 | 상태 |
|----------|------|------|
| 세션 검색 | 키워드 기반 대화 검색 | 📝 예정 |
| 세션 요약 | AI 기반 대화 요약 | 📝 예정 |
| 상담 통계 대시보드 | 사용자별 이용 통계 | 📝 예정 |
| 푸시 알림 | 세션 이벤트 알림 | 📝 예정 |

## 참고 사항
- OAuth 제공자는 Google과 Kakao만 지원하며, Naver 연동은 제거되었다.
- ResponseAspect는 운영 환경에서만 HTTP Status를 조정하며, 개발/테스트 프로필에서는 모든 응답이 200 OK로 반환된다.
- Expo 앱은 위 유스케이스를 탭 구조(진행 / 종료 / 북마크)로 노출하고 React Query로 캐시를 관리한다.
