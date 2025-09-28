# Refresh Token 도입 및 개선 작업 계획

## 개요
현재 백엔드는 Access Token과 Refresh Token을 모두 발급하지만, Refresh Token을 서버 측에서 저장/검증하지 않는다. 클라이언트는 `/api/auth/refresh` 엔드포인트가 실패하면 로그아웃 처리하도록 구성돼 있으므로, 백엔드가 Refresh Token을 관리하도록 보완하면 안전한 30일 토큰 전략을 구축할 수 있다.

## 백엔드 작업

### 1. 저장소 구성
- **선택**: Redis 사용 (속도·TTL 관리 유리)
- key 예시: `refresh-token:{tokenId}`
- 저장 정보: `userId`, `refreshToken`, `deviceInfo`, `issuedAt`, `expiresAt`
- TTL: 30일 (`Duration.ofDays(30)`)
- 삭제/업데이트를 쉽게 하기 위해 `userId -> tokenId` 맵핑도 저장

### 2. AuthService 변경
- 로그인/회원가입 후 `createAuthResponse` 호출 전에 `refreshTokenStore.save(userId, refreshToken, ttl, deviceMeta)` 추가
- `/auth/refresh`에서 JWT 서명·만료 체크 뒤 **저장소에서 존재 확인**
- 토큰 롤링 전략 적용: Refresh 사용 시 새 토큰 발급 후 기존 토큰 삭제/대체
- Refresh 실패 시 `UnauthorizedException` 던져서 프론트가 로그아웃하도록 유지

### 3. 저장소 인터페이스/구현 작성
```kotlin
interface RefreshTokenStore {
    fun save(userId: Long, token: String, ttl: Duration, deviceMeta: DeviceMeta?)
    fun find(token: String): RefreshTokenRecord?
    fun delete(token: String)
    fun deleteAllByUserId(userId: Long)
    fun update(userId: Long, oldToken: String, newToken: String, ttl: Duration)
}
```
- Redis 구현(예시): `RefreshTokenRedisStore`
- `DeviceMeta`: userAgent, ip address 등 필요 시 확장

### 4. 로그아웃/강제 만료 처리
- `/auth/logout` 신규 엔드포인트 추가해 Refresh Token 삭제
- 계정 삭제/정지 시 `deleteAllByUserId` 호출로 전체 Refresh Token 폐기
- 비밀번호 변경 시에도 토큰 무효화 고려

### 5. 설정값 확인
- `jwt.refresh-expiration` 30일(2592000000 ms)로 설정
- Access Token 만료는 30분 등 짧게 유지

### 6. 보안 & 모니터링
- Refresh 재발급 실패 로깅
- IP/User-Agent 비교로 위험한 사용 탐지
- Redis 모니터링, 재시도 전략(필요 시) 준비
- Refresh Token 조회 시 rate-limit 적용 검토

## 프론트엔드 작업 상황
- `services/api.ts`에서 401 → `/auth/refresh` → 토큰 저장 → 원 요청 재시도 로직이 구현되어 있음
- 로그인 시 토큰·유저 정보 AsyncStorage에 저장 (`authStore.login`)
- Refresh 토큰 갱신 성공 시 Access Token/Refresh Token 교체 (`authStore.setTokens`)
- Refresh 실패 시 자동 로그아웃

### 추가 확인 사항
- Refresh 토큰 저장 위치(AsyncStorage)에 대한 보안 정책 검토 (HTTP-only Cookie 등으로 이전 가능)
- `/auth/refresh` 실패 시 로그인 화면으로 안내하는 화면 UX 확인

## 향후 확장 아이디어
- 여러 디바이스 지원 시 `userId+deviceId` 조합으로 저장
- Refresh Token 블랙리스트(고위험 토큰) 운영
- Access Token 검증 API(`/auth/verify`) 추가해 앱 시작 시 유효성 확인
- 모니터링: Refresh 재발급 시도 현황, Redis 키 만료/누락 체크

---
이 문서에 따라 Redis 저장소 작성 → AuthService 수정 → 로그아웃/강제 만료 API 보완 → 프론트 상호 테스트 후 배포하면 Refresh Token 30일 전략을 안정적으로 운영할 수 있다.
