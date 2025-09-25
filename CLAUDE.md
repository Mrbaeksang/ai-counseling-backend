# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Counseling App - A Spring Boot Kotlin application providing AI-powered philosophical counseling services through integration with OpenRouter API. The system allows users to have 1-on-1 conversations with AI counselors embodying historical philosophers and thinkers.

## Recent Updates (2025-09-08)

- **Auth Domain Migration**: Auth functionality moved from `domain/auth` to `global/auth` for better architectural separation
- **OAuth Token Verifiers**: Implemented unified `OAuthTokenVerifier` interface with provider-specific implementations
- **Test Organization**: Restructured auth tests with base test class and provider-specific test files
- **JDSL 3.5.5**: Upgraded to latest version for type-safe JPQL queries

## Development Commands

### Build & Run
```bash
# Run application
./gradlew bootRun

# Build project
./gradlew build

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*.UserServiceTest"

# Run with coverage
./gradlew jacocoTestReport
```

### Code Quality
```bash
# Ktlint check (code style)
./gradlew ktlintCheck

# Ktlint format (auto-fix style issues)
./gradlew ktlintFormat

# Detekt analysis (code quality)
./gradlew detekt

# Run all quality checks
./gradlew check-all

# Install git pre-commit hooks
./gradlew installGitHooks
```

## Architecture & Code Organization

### Domain-Driven Design Structure
The application follows DDD principles with clear bounded contexts:

- **domain/** - Core business logic organized by aggregate roots
  - Each domain module contains: controller, dto, entity, repository, service
  - Key domains: user, counselor, session
  
- **global/** - Cross-cutting concerns and infrastructure
  - auth: OAuth2 authentication system (controller, dto, service)
  - config: Spring configurations (Security, CORS, WebClient, JDSL, Swagger)
  - security: JWT authentication components (JwtTokenProvider, JwtAuthenticationFilter, AuthProvider)
  - openrouter: AI API integration layer
  - exception: Global error handling
  - rsData/rq: Response/Request wrapper patterns
  - aspect: AOP components (ResponseAspect)
  - pagination: Pagination utilities
  - constants: Application-wide constants

### Global Components Detail

#### AOP (Aspect-Oriented Programming)
- **ResponseAspect** (`global/aspect/ResponseAspect.kt`)
  - Intercepts all Controller methods returning RsData
  - Automatically sets HTTP status codes based on result codes
  - MUST be disabled in tests via `@Profile("!test")` to prevent ClassCastException
  - S-* codes (success) → HTTP 200, F-* codes (failure) → appropriate HTTP status

#### Response/Request Wrappers
- **RsData** (`global/rsData/RsData.kt`)
  - Standard response wrapper: `RsData<T>(resultCode, msg, data)`
  - Result codes: S-* for success, F-* for failure
  - Factory methods: `RsData.of()` for success, `RsData.failOf()` for errors

- **Rq** (`global/rq/Rq.kt`)
  - Request context holder injected via `@Component` and `@RequestScope`
  - Provides: `currentUserId` (from JWT), `currentUser` (for OAuth2), `isAuthenticated`

#### Authentication System (global/auth)
- **OAuth Providers**: Google, Kakao, Naver
- All providers implement `OAuthTokenVerifier` interface
- JWT tokens (access & refresh) generated after successful OAuth login
- Provider ID + Auth Provider combination uniquely identifies users

### Key Architectural Patterns

1. **Layered Architecture**: Controller → Service → Repository
2. **Response Wrapper Pattern**: All API responses use RsData wrapper
3. **Base Entity Pattern**: Common fields (id, timestamps) in BaseEntity
4. **JWT + OAuth2**: Dual authentication strategy supporting social logins
5. **Reactive Programming**: WebFlux for non-blocking OpenRouter API calls
6. **Custom Repository Pattern**: Complex queries via `RepositoryCustom` + `RepositoryImpl` using JDSL

### Database Strategy
- JPA with Kotlin JDSL 3.5.5 for type-safe queries
- H2 for development, PostgreSQL for production
- Entity relationships properly mapped with lazy loading
- Auditing enabled via BaseEntity and @EnableJpaAuditing

## API Integration Points

### OpenRouter AI Service
- Configuration in `OpenRouterService.kt`
- Async/reactive calls using WebClient
- Default AI Model: `meta-llama/llama-3.2-3b-instruct`
- Message context management for conversations

## Testing Approach

### Testing Stack
- **Spring Boot 3.5.4** with **Kotlin 1.9.25** and **JDK 21**
- **MockK 1.13.8**: Kotlin-first mocking library (NOT Mockito)
- **SpringMockK 4.0.2**: Spring integration for MockK

### Critical Configuration for Spring Boot 3.5 + Kotlin

#### 1. AOP and Test Profile Separation
```kotlin
@Aspect
@Component
@Profile("!test")  // Critical: Prevents ClassCastException in tests
class ResponseAspect { ... }
```

#### 2. JDK 21 Compatibility
```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "-Djdk.instrument.traceUsage=false"
    )
}
```

### Test Organization
Each controller has multiple focused test files:
- Base test class for common setup (e.g., `ChatSessionControllerBaseTest.kt`)
- One test file per API endpoint (max 150 lines for Detekt compliance)
- `@MockkBean` for mocking services

### Common Testing Pitfalls & Solutions

| Problem | Solution |
|---------|----------|
| ClassCastException: ResponseEntity cannot be cast to RsData | Add `@Profile("!test")` to ResponseAspect |
| Mockito warnings in JDK 21 | Add JVM args: `-XX:+EnableDynamicAgentLoading` |
| Wrong resultCode format | Use F- prefix for failures (e.g., F-401) |

## Environment Configuration

Required environment variables (.env file):
- `OPENROUTER_API_KEY` - OpenRouter API key
- `JWT_SECRET` - JWT signing secret
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - Database credentials (production)

Spring profiles:
- `dev` - H2 in-memory database, debug logging
- `prod` - PostgreSQL, optimized settings
- `test` - Test configuration with mocked services

## Code Style Guidelines

### Kotlin Style
- Line length limit: 120 characters
- No wildcard imports except java.util.*
- Naming: PascalCase for classes, camelCase for functions/variables

### Comment Placement Rules (Ktlint)
- **NEVER place comments inside value argument lists**
- **ALWAYS place comments on separate lines above the code**
- Incorrect: `val rating = 10, // comment ❌`
- Correct: Place comment on line above

## Git Workflow & Commit Rules

### Branch Strategy
- **NEVER push directly to main branch**
- **ALWAYS create feature branch and PR**
- Branch naming: `feat/`, `fix/`, `refactor/`, `test/`, `docs/`

### Commit Messages
- **MUST use Korean commit messages**
- Format: `type: 한국어 설명`
- Example: `fix: 로그인 인증 버그 수정`

## N+1 Query Problem Solutions

When dealing with N+1 queries, use the Custom Repository pattern with JDSL:

```kotlin
// 1. Create Custom interface
interface ChatSessionRepositoryCustom {
    fun findSessionsWithCounselor(...): Page<SessionListResponse>
}

// 2. Implement with JDSL (no @Repository annotation)
class ChatSessionRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor
) : ChatSessionRepositoryCustom {
    override fun findSessionsWithCounselor(...): Page<SessionListResponse> {
        return kotlinJdslJpqlExecutor.findPage(pageable) {
            selectNew<SessionListResponse>(...)
                .from(entity(ChatSession::class))
                .join(Counselor::class)
        }
    }
}

// 3. Main repository extends both interfaces
@Repository
interface ChatSessionRepository : 
    JpaRepository<ChatSession, Long>, 
    ChatSessionRepositoryCustom
```

## Common Development Tasks

When implementing new features:
1. Create entity in appropriate domain package
2. Add repository interface extending JpaRepository
3. Implement service with business logic
4. Create DTOs for request/response
5. Add controller with proper validation
6. Write unit and integration tests
7. Run ktlint and detekt before committing