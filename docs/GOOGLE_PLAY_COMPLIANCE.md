# Google Play 정책 준수 가이드 (2025-09-24)

> 개인 개발자 계정으로 Entertainment 카테고리 심사를 통과하기 위한 백엔드/운영 로드맵

---

## 1. 현재까지 완료된 조치

| 구분 | 내용 | 상태 |
|------|------|------|
| 카테고리/브랜딩 | Health → Entertainment, “상담/치료” 표현 제거 | ✅ 완료 |
| 도메인 네이밍 | `counselor` → `character` (패키지, 엔티티, 테이블, 캐시, DTO) | ✅ 완료 |
| 챗 세션 로직 | 단계(enum), `shouldEnd`, 품질 경고 제거. 캐릭터 베이스 프롬프트 + 안전 안내만 사용 | ✅ 완료 |
| 테스트/시드 | 기본 응답 JSON을 `content`/`title` 중심으로 정리 | ✅ 완료 |

---


## 2. 남은 필수 작업: 신고(Report) 시스템 구축

Google Play AI Generated Content 정책에 따라, 사용자가 부적절한 AI 응답을 신고하고 운영자가 처리 이력을 남길 수 있는 기능이 반드시 필요합니다.

### 2.1 데이터 모델 제안

- **추가 테이블 `message_reports`**
  | 필드 | 설명 |
  |------|------|
  | `id` (PK) | 신고 ID |
  | `message_id` (FK) | 신고 대상 메시지 |
  | `reporter_user_id` (FK) | 신고자 |
  | `category` (enum) | `SAFETY`, `HARMFUL`, `OFFENSIVE`, `SPAM`, `OTHER` |
  | `description` | 상세 사유(선택) |
  | `status` (enum) | `PENDING`, `IN_REVIEW`, `RESOLVED`, `REJECTED` |
  | `handled_by` | 처리 담당자 ID(선택) |
  | `handled_at` | 처리 시각 |
  | `resolution_note` | 조치/판단 메모 |
  | `created_at` | 신고 생성 시각 |

- `MessageReport` 엔티티 + `MessageReportRepository`
- `ReportCategory`, `ReportStatus` enum 정의
- `ReportService`: 신고 생성, 중복 여부 확인, 상태 변경 로직 제공

### 2.2 API 흐름

| Method | 경로 | 설명 |
|--------|------|------|
| POST | `/api/sessions/{sessionId}/messages/{messageId}/report` | 사용자 신고 생성 |
| GET | `/api/admin/reports` | (운영자) 신고 목록 조회, 필터: 상태/카테고리/기간 |
| GET | `/api/admin/reports/{reportId}` | 신고 상세 |
| PATCH | `/api/admin/reports/{reportId}` | 상태 변경 + 메모 등록 |

- `ReportRequest` (사용자) : `category`, `description`
- `ReportUpdateRequest` (운영자) : `status`, `resolutionNote`
- 신고가 생성되면 메시지 `is_reported` 플래그를 `true`로 마킹 → FE에서 시각적 표시 가능

### 2.3 챗 서비스와의 연계

- `ChatSessionService`는 신고 API와 직접 엮지 않고, 메시지 검증이나 제목 업데이트만 수행
- 이후 기능(재응답 요청, 메시지 숨김 등)은 신고 처리 결과에 따라 운영자 도구에서 실행

### 2.4 운영/감사 요구

- 신고 처리 로그(상태 전환, 담당자, 메모) 보관 → 감사 대응 가능해야 함
- 심각 신고(`SAFETY`) 발생 시 Slack/Webhook 등 알림 채널 연동 고려
- 신고 통계 리포트(일/주 단위) 준비해 스토어 심사 시 증빙 가능하도록 함

---

## 3. 추가 정책 대응 항목

1. **고정 안내 문구**: 앱/웹 UI에서 “AI가 생성한 엔터테인먼트 대화이며 사실과 다를 수 있음, 위기 시 1393·112 등 전문 기관에 연락” 메시지를 상시 노출
2. **사용자 가이드와 FAQ**: 신고 버튼 위치, 신고 처리 절차, 응급 상황 안내를 Help 센터에 정리
3. **데이터 보존 정책**: 신고 데이터, 로그 보관 기간(예: 1년)과 파기 절차 문서화

---

## 4. 구현 로드맵 (개정)

| 단계 | 작업 | 결과물 |
|------|------|--------|
| 1 | `message_reports` 엔티티·Repository·마이그레이션 스크립트 작성 | DDL, Kotlin 엔티티 |
| 2 | `ReportService` + 예외/권한 처리 | 서비스/도메인 코드 |
| 3 | 사용자 신고 API + 통합 테스트 | 컨트롤러/DTO/테스트 |
| 4 | 운영자용 신고 관리 API + 테스트 | 관리용 컨트롤러 |
| 5 | Swagger/문서 업데이트, 운영 가이드 작성 | 정책 문구, 화면 시나리오 |

---

## 5. 성공 체크리스트

- [ ] 사용자가 어떤 메시지든 즉시 신고 가능
- [ ] 신고 후 중복 방지 및 상태 추적 가능 (`PENDING → IN_REVIEW → RESOLVED/REJECTED`)
- [ ] 운영자 API에서 필터·정렬·상세 조회 지원
- [ ] 모든 신고·조치 로그가 DB에 보관되고 감사 대응 가능
- [ ] UI와 문서에 안전 고지·신고 안내가 명확히 반영

---

_최종 업데이트: 2025-09-24_

담당: Claude Code Assistant
