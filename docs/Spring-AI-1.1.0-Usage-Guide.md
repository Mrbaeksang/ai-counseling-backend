# Spring AI 1.1.0 사용법 가이드

> **주의**: 이 문서는 Spring AI 1.1.0-SNAPSHOT 버전을 기반으로 작성되었으며, 아직 안정적인 릴리스가 아닙니다. 프로덕션 환경에서는 안정적인 버전 (1.0.2) 사용을 권장합니다.

## 목차
1. [개요](#개요)
2. [핵심 API](#핵심-api)
3. [ChatClient API](#chatclient-api)
4. [Advisors API](#advisors-api)
5. [Chat 모델 통합](#chat-모델-통합)
6. [Prompt API](#prompt-api)
7. [Structured Output Converter](#structured-output-converter)
8. [Multimodality API](#multimodality-api)
9. [Chat Memory API](#chat-memory-api)
10. [Tools API](#tools-api)
11. [Vector Database API](#vector-database-api)
12. [Embeddings API](#embeddings-api)
13. [ETL Pipeline API](#etl-pipeline-api)
14. [MCP Client Boot Starter](#mcp-client-boot-starter)
15. [실전 예제](#실전-예제)

## 개요

Spring AI 1.1.0은 AI 모델과의 통합을 위한 포괄적인 프레임워크입니다. 주요 특징:

- **다양한 AI 모델 지원**: OpenAI, Anthropic, Azure OpenAI, Google Vertex AI 등
- **멀티모달 지원**: 텍스트, 이미지, 오디오 처리
- **벡터 데이터베이스 통합**: 14개 벡터 데이터베이스 지원
- **RAG (Retrieval Augmented Generation) 지원**
- **Spring Boot 자동 구성**

## 핵심 API

### 주요 인터페이스
- `ChatModel`: 채팅 기반 AI 모델 인터페이스
- `EmbeddingModel`: 임베딩 생성 인터페이스
- `VectorStore`: 벡터 데이터베이스 인터페이스
- `ChatClient`: 고수준 채팅 클라이언트

## ChatClient API

ChatClient는 AI 모델과의 상호작용을 위한 플루언트 API를 제공합니다.

### 기본 설정

```java
@RestController
class MyController {
    private final ChatClient chatClient;

    public MyController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    String generation(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .call()
            .content();
    }
}
```

### 응답 유형
- `content()`: 응답 텍스트 반환
- `chatResponse()`: 메타데이터가 포함된 전체 응답
- `entity()`: Java 타입으로 변환된 응답
- `stream()`: 반응형 스트리밍 응답

### 예제: 스트리밍 응답

```java
Flux<String> streamResponse = chatClient.prompt()
    .user("Tell me a story")
    .stream()
    .content();
```

## Advisors API

Advisors는 AI 상호작용을 가로채고 수정할 수 있는 메커니즘을 제공합니다.

### 핵심 인터페이스
- `CallAdvisor`: 동기식 상호작용용
- `StreamAdvisor`: 반응형/스트리밍 상호작용용

### 커스텀 Advisor 구현

```java
public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logRequest(request);
        ChatClientResponse response = chain.nextCall(request);
        logResponse(response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        logRequest(request);
        return chain.nextStream(request)
            .doOnNext(this::logResponse);
    }
}
```

### 내장 Advisors
- Chat Memory Advisors
- Question Answering Advisor
- Reasoning Advisor
- Content Safety Advisor

## Chat 모델 통합

### OpenAI 설정

```properties
spring.ai.openai.api-key=YOUR_API_KEY
spring.ai.openai.chat.options.model=gpt-4o
spring.ai.openai.chat.options.temperature=0.7
```

### 지원 모델들
- **OpenAI**: gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo
- **Anthropic**: Claude 3 시리즈
- **Azure OpenAI**
- **Google Vertex AI / GenAI**
- **MistralAI**
- **Ollama**
- **기타**: Groq, Perplexity, Hugging Face 등

## Prompt API

### 핵심 구성요소
- `Prompt`: 메시지와 옵션의 컨테이너
- `Message`: 역할별 콘텐츠 (system, user, assistant, tool)
- `PromptTemplate`: 변수 치환이 가능한 구조화된 프롬프트

### 메시지 역할
- **System**: AI 동작과 응답 스타일 설정
- **User**: 사용자 입력
- **Assistant**: AI 응답
- **Tool/Function**: 도구 호출의 추가 정보

### 예제

```java
PromptTemplate template = new PromptTemplate("Tell me about {topic}");
Prompt prompt = template.create(Map.of("topic", "Spring AI"));
```

## Structured Output Converter

LLM의 텍스트 출력을 구조화된 형식으로 변환합니다.

### 주요 컨버터
- `BeanOutputConverter`: Java 클래스/레코드로 변환
- `MapOutputConverter`: Map<String, Object>로 변환
- `ListOutputConverter`: 리스트로 변환

### 사용 예제

```java
ActorsFilms actorsFilms = ChatClient.create(chatModel).prompt()
    .user("Generate filmography for Tom Hanks")
    .call()
    .entity(ActorsFilms.class);
```

### 지원 모델
- OpenAI
- Anthropic Claude 3
- Azure OpenAI
- Mistral AI
- Ollama
- Vertex AI Gemini

## Multimodality API

여러 데이터 유형(텍스트, 이미지, 오디오)을 동시에 처리할 수 있습니다.

### 지원 모델
- Anthropic Claude 3
- AWS Bedrock Converse
- Azure OpenAI (GPT-4o)
- Mistral AI
- Ollama
- OpenAI
- Vertex AI Gemini

### 사용 예제

```java
var userMessage = new UserMessage(
    "이 사진에서 무엇을 보시나요?",
    new Media(MimeTypeUtils.IMAGE_PNG, imageResource)
);
```

## Chat Memory API

대화 컨텍스트를 유지하기 위한 메모리 관리 시스템입니다.

### 메모리 유형
1. **Message Window Chat Memory**: 메시지 창 유지 (기본 최대 20개)

### 저장소 옵션
- In-Memory Repository (기본값)
- JDBC Repository
- Cassandra Repository
- Neo4j Repository

### 사용 예제

```java
ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
String conversationId = "007";

chatClient.prompt()
    .user("코딩 라이센스가 있나요?")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .call()
    .content();
```

## Tools API

AI 모델이 외부 API나 도구와 상호작용할 수 있게 해줍니다.

### 사용 사례
1. **정보 검색**: 외부 데이터 가져오기
2. **액션 실행**: 소프트웨어 시스템에서 작업 수행

### 도구 정의 방법

#### 메소드를 도구로 사용

```java
class WeatherTools {
    @Tool(description = "특정 위치의 현재 날씨 정보를 가져옵니다")
    WeatherResponse getCurrentWeather(String location) {
        // 구현
    }
}
```

#### Function을 도구로 사용
- `Function`, `Supplier`, `Consumer`, `BiFunction` 지원
- 프로그래밍 방식 또는 Spring 빈으로 정의 가능

## Vector Database API

벡터 임베딩 저장 및 검색을 위한 특화된 데이터베이스 API입니다.

### 핵심 인터페이스
1. `VectorStoreRetriever`: 읽기 전용 문서 검색
2. `VectorStore`: 읽기/쓰기 기능 포함

### 주요 기능
- 유사성 검색 (정확한 매치 대신)
- 메타데이터 기반 필터링
- 배치 처리 전략

### 사용 예제

```java
// 문서 추가
List<Document> documents = jsonReader.get();
vectorStore.add(documents);

// 문서 검색
List<Document> similarDocs = retriever.similaritySearch(userQuery);
```

### 지원 데이터베이스
Pinecone, Milvus, Redis 등 14개 벡터 데이터베이스 지원

## Embeddings API

텍스트, 이미지, 비디오를 벡터 표현으로 변환합니다.

### 핵심 인터페이스: `EmbeddingModel`
- `embed(String text)`: 단일 텍스트 임베딩
- `embed(List<String> texts)`: 배치 임베딩

### 주요 구성요소
- `EmbeddingRequest`: 입력 텍스트와 옵션
- `EmbeddingResponse`: 생성된 임베딩과 메타데이터
- `Embedding`: 단일 임베딩 벡터

### 지원 구현체
- OpenAI
- Azure OpenAI
- Ollama
- Vertex AI
- Mistral AI 등

## ETL Pipeline API

RAG를 위한 데이터 준비를 위한 Extract, Transform, Load 파이프라인입니다.

### 핵심 구성요소
1. `DocumentReader`: 다양한 소스에서 문서 추출
2. `DocumentTransformer`: 문서 처리 및 수정
3. `DocumentWriter`: 처리된 문서 저장

### 지원 문서 형식
- JSON, Text, HTML, Markdown, PDF, Tika (다중 형식)

### 변환 기능
- **텍스트 분할**: AI 모델의 컨텍스트 윈도우에 맞게 텍스트 청크 분할
- **메타데이터 강화**: 키워드 추출, 요약 생성

### 사용 예제

```java
vectorStore.accept(
  tokenTextSplitter.apply(
    pdfReader.get()
  )
)
```

## MCP Client Boot Starter

Model Context Protocol (MCP) 클라이언트 기능을 위한 Spring Boot 자동 구성을 제공합니다.

### 주요 기능
- 동기식 및 비동기식 클라이언트 구현 지원
- 다양한 전송 유형 지원 (STDIO, HTTP/SSE, Streamable HTTP)
- 다중 클라이언트 인스턴스 관리

### 설정 예제

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: my-mcp-client
        type: SYNC
        sse:
          connections:
            server1:
              url: http://localhost:8080
```

### 주요 어노테이션
- `@McpLogging`
- `@McpSampling`
- `@McpElicitation`
- `@McpProgress`
- `@McpToolListChanged`

## 실전 예제

### 1. RAG 시스템 구축

```java
@Service
public class RagService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
            .defaultAdvisors(
                QuestionAnswerAdvisor.builder(vectorStore).build()
            )
            .build();
    }

    public String answer(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

### 2. 멀티모달 채팅봇

```java
@RestController
public class MultimodalChatController {
    private final ChatClient chatClient;

    @PostMapping("/chat/multimodal")
    public String chatWithImage(
        @RequestParam String text,
        @RequestParam MultipartFile image) throws IOException {

        Resource imageResource = new ByteArrayResource(image.getBytes());

        return chatClient.prompt()
            .user(userMessage -> userMessage
                .content(text)
                .media(MimeTypeUtils.IMAGE_JPEG, imageResource))
            .call()
            .content();
    }
}
```

### 3. 도구가 있는 AI 에이전트

```java
@Component
public class WeatherAgent {

    @Tool("특정 도시의 현재 날씨를 가져옵니다")
    public String getCurrentWeather(String city) {
        // 실제 날씨 API 호출
        return "현재 " + city + "의 날씨는 맑습니다.";
    }

    @Tool("여행 추천을 제공합니다")
    public String getTravelRecommendation(String destination, String weather) {
        return destination + "에 " + weather + " 날씨에 적합한 활동을 추천합니다.";
    }
}
```

### 4. 대화 메모리가 있는 챗봇

```java
@Service
public class ConversationalChatService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ConversationalChatService(ChatClient.Builder builder) {
        this.chatMemory = MessageWindowChatMemory.builder()
            .maxSize(10)
            .build();

        this.chatClient = builder
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }

    public String chat(String userId, String message) {
        return chatClient.prompt()
            .user(message)
            .advisors(advisor ->
                advisor.param(ChatMemory.CONVERSATION_ID, userId))
            .call()
            .content();
    }
}
```

## 주요 팁과 모범 사례

### 1. 성능 최적화
- 배치 처리를 위해 `TokenCountBatchingStrategy` 사용
- 적절한 벡터 스토어 선택
- 스트리밍 API 활용으로 응답성 향상

### 2. 보안 고려사항
- API 키를 환경 변수로 관리
- 민감한 정보 로깅 방지
- 입력 검증 및 출력 필터링

### 3. 에러 처리
- 모델별 제한사항 고려
- 재시도 로직 구현
- 폴백 메커니즘 준비

### 4. 모니터링과 관찰가능성
- Advisor를 통한 로깅 구현
- 메트릭 수집
- 비용 추적

## 결론

Spring AI 1.1.0은 AI 애플리케이션 개발을 위한 포괄적이고 유연한 프레임워크입니다. 다양한 AI 모델, 벡터 데이터베이스, 멀티모달 기능을 통합하여 현대적인 AI 애플리케이션을 쉽게 구축할 수 있습니다.

이 가이드의 예제와 패턴을 활용하여 RAG 시스템, 멀티모달 챗봇, AI 에이전트 등 다양한 AI 애플리케이션을 개발할 수 있습니다.