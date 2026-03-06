.PHONY: help run dev stop infra infra-down infra-logs monitoring monitoring-down \
       build test clean compile docker-build docker-run \
       db-shell redis-cli logs

# ============================================================
# AI Gateway - Makefile
# ============================================================

DOCKER_COMPOSE = docker compose -f docker/docker-compose.yml
GRADLE = ./gradlew

## -------------------- Help --------------------

help: ## 사용 가능한 명령어 목록
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

## -------------------- 로컬 실행 --------------------

run: infra ## 인프라 + 애플리케이션 실행 (한번에)
	$(GRADLE) bootRun --args='--spring.profiles.active=local'

dev: ## 애플리케이션만 실행 (인프라가 이미 떠있을 때)
	$(GRADLE) bootRun --args='--spring.profiles.active=local'

stop: infra-down ## 인프라 중지

## -------------------- 인프라 (MongoDB, Redis) --------------------

infra: ## MongoDB + Redis + Mongo Express 시작
	$(DOCKER_COMPOSE) up -d mongodb redis mongo-express
	@echo ""
	@echo "✅ 인프라 시작 완료"
	@echo "  MongoDB:       localhost:27017"
	@echo "  Redis:         localhost:6379"
	@echo "  Mongo Express: http://localhost:8081"

infra-down: ## 인프라 중지
	$(DOCKER_COMPOSE) down

infra-logs: ## 인프라 로그 확인
	$(DOCKER_COMPOSE) logs -f mongodb redis

## -------------------- 모니터링 (Prometheus, Grafana) --------------------

monitoring: ## Prometheus + Grafana 시작 (인프라 포함)
	$(DOCKER_COMPOSE) --profile monitoring up -d
	@echo ""
	@echo "✅ 모니터링 시작 완료"
	@echo "  Prometheus: http://localhost:9090"
	@echo "  Grafana:    http://localhost:3001 (admin/admin)"

monitoring-down: ## 모니터링 포함 전체 중지
	$(DOCKER_COMPOSE) --profile monitoring down

## -------------------- 빌드 & 테스트 --------------------

build: ## 프로젝트 빌드 (테스트 포함)
	$(GRADLE) build

compile: ## 컴파일만 (빠른 확인)
	$(GRADLE) compileKotlin

test: ## 테스트 실행
	$(GRADLE) test

clean: ## 빌드 캐시 정리
	$(GRADLE) clean

## -------------------- Docker --------------------

docker-build: ## Docker 이미지 빌드
	docker build -f docker/Dockerfile -t ai-gateway:latest .

docker-run: ## Docker 컨테이너 실행
	docker run --rm -p 8080:8080 \
		--network host \
		-e SPRING_PROFILES_ACTIVE=local \
		-e ANTHROPIC_API_KEY=$${ANTHROPIC_API_KEY} \
		-e OPENAI_API_KEY=$${OPENAI_API_KEY} \
		ai-gateway:latest

## -------------------- 유틸리티 --------------------

db-shell: ## MongoDB 쉘 접속
	docker exec -it $$(docker ps -qf "ancestor=mongo:8.0") mongosh ai_gateway

redis-cli: ## Redis CLI 접속
	docker exec -it $$(docker ps -qf "ancestor=redis:7-alpine") redis-cli

logs: ## 애플리케이션 로그 (docker compose)
	$(DOCKER_COMPOSE) logs -f

## -------------------- 기본 --------------------

.DEFAULT_GOAL := help
