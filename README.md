# AI Gateway

LLM API Proxy & Orchestration Platform

여러 LLM API(Claude, GPT, Gemini)를 통합 관리하는 게이트웨이 서비스. Failover, Rate Limiting, 캐싱, 스마트 라우팅, Observability를 제공하여 AI 연계 서비스의 안정성과 비용 효율성을 확보합니다.

## Tech Stack

| 구분 | 기술 |
|------|------|
| Language | Kotlin 2.3.10 + Java 21 |
| Framework | Spring Boot 4.0.3 (WebFlux + Coroutine) |
| Database | MongoDB 8.0 (Reactive) |
| Cache / Queue | Redis 7 |
| Build | Gradle 8.14 (Kotlin DSL) |
| Resilience | Resilience4j + Spring 7 내장 |
| Monitoring | Micrometer + Prometheus + Grafana |
| Test | JUnit 5 + MockK + Testcontainers + WireMock |

## Features

- **Unified API** - OpenAI 호환 포맷의 단일 엔드포인트로 여러 LLM 호출
- **Failover & Circuit Breaker** - LLM API 장애 시 자동 대체 모델 전환
- **Rate Limiting** - RPM/TPM 기반 다층 요청 제한 (Redis Lua Script)
- **Response Caching** - Exact Match + Semantic Cache로 비용 절감
- **Smart Routing** - 요청 특성에 따라 최적 모델 자동 선택
- **Streaming Proxy** - SSE 스트리밍 응답 프록시 + 토큰 카운팅
- **Observability** - 실시간 메트릭 (TTFT, 토큰/초, 에러율, 비용)
- **Budget Control** - 팀/프로젝트별 예산 할당 및 상한 관리

## Architecture

```
Client → AuthFilter → RateLimitFilter → ProxyController
  → ProxyService
    → CacheManager (캐시 확인)
    → SmartRouter (모델 선택)
    → CircuitBreaker (장애 감지)
      → LlmClient (Claude / OpenAI / Gemini)
    → FailoverHandler (fallback)
    → RequestLogService (비동기 로그)
    → MetricsCollector (메트릭)
  → Response (OpenAI 호환 포맷)
```

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run Infrastructure

```bash
cd docker
docker compose up -d
```

MongoDB (27017), Redis (6379), Mongo Express (8081)이 시작됩니다.

### Run Application

```bash
# 환경변수 설정
export ANTHROPIC_API_KEY=your-key-here
export OPENAI_API_KEY=your-key-here

# 실행
./gradlew bootRun
```

Application: http://localhost:8080

### Build

```bash
./gradlew build
```

### Test

```bash
./gradlew test
```

### Docker Build

```bash
docker build -f docker/Dockerfile -t ai-gateway .
```

## API Reference

### Proxy API (OpenAI Compatible)

```bash
# Chat Completions
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### Admin API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/api-keys` | API Key 생성 |
| GET | `/api/v1/admin/api-keys` | API Key 목록 |
| PUT | `/api/v1/admin/api-keys/{id}` | API Key 수정 |
| DELETE | `/api/v1/admin/api-keys/{id}` | API Key 비활성화 |
| GET | `/api/v1/admin/models` | 모델 목록 및 상태 |
| PUT | `/api/v1/admin/models/{id}` | 모델 설정 수정 |
| GET | `/api/v1/admin/usage` | 사용량 통계 |
| GET | `/api/v1/admin/health` | 시스템 헬스체크 |

## Project Structure

```
src/main/kotlin/com/gateway/
├── api/
│   ├── proxy/          # Proxy Controller (chat/completions)
│   ├── admin/          # Admin Controller
│   ├── dto/            # Request/Response DTOs
│   ├── filter/         # Auth, RateLimit Filter
│   └── exception/      # Global Exception Handler
├── domain/
│   ├── model/          # MongoDB Documents
│   ├── repository/     # Reactive Repositories
│   └── service/        # Business Logic
├── core/
│   ├── routing/        # Smart Router
│   ├── ratelimit/      # Rate Limiter
│   ├── cache/          # Cache Manager
│   ├── queue/          # Request Queue
│   └── circuit/        # Circuit Breaker
├── infrastructure/
│   ├── llm/            # LLM Clients (Claude, OpenAI, Gemini)
│   ├── streaming/      # SSE Stream Proxy
│   ├── metrics/        # Prometheus Metrics
│   └── alert/          # Slack Alerts
└── config/             # Spring Configuration
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ANTHROPIC_API_KEY` | Yes | Anthropic API Key |
| `OPENAI_API_KEY` | Yes | OpenAI API Key |
| `GOOGLE_API_KEY` | No | Google AI API Key |
| `SPRING_DATA_MONGODB_URI` | No | MongoDB URI (default: `mongodb://localhost:27017/ai_gateway`) |
| `SPRING_DATA_REDIS_HOST` | No | Redis Host (default: `localhost`) |
| `SLACK_WEBHOOK_URL` | No | Slack Webhook for alerts |

## Related

- [ai-gateway-dashboard](https://github.com/rimi-dev/ai-gateway-dashboard) - Admin & Monitoring Dashboard (Next.js)

## License

MIT
