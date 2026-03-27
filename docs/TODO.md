# 개발 TODO 리스트

> 마지막 업데이트: 2026-03-27  
> 기준: 현재 Spring Boot 골격 코드 → 태아 기록 + 분석 플랫폼 MVP

---

## 진행 상태 범례

- `[ ]` 미완료
- `[x]` 완료
- `[-]` 진행중
- `[!]` 블로커 (선행 작업 필요)

---

## PHASE 0 — 현재 코드 정리 (즉시)

### 인프라 / 설정
- [x] `application.properties` → `application.yml` 전환 (가독성)
- [x] `application-dev.yml` / `application-prod.yml` 분리 (환경별 설정)
- [x] DB 비밀번호 하드코딩 제거 → 환경변수 처리 (`${DB_PASSWORD}`)
- [x] `.gitignore`에 `application-prod.yml` 추가

### 기존 코드 보완
- [x] `PredictionResult` 엔티티에 `analysisStatus` 필드 추가 (`PENDING` / `DONE` / `FAILED`)
- [x] 글로벌 예외 핸들러 추가 (`@RestControllerAdvice`)
- [x] API 응답 공통 포맷 정의 (`ApiResponse<T>` 래퍼 클래스)
- [x] `SignupRequest` 유효성 검사 메시지 한글화

---

## PHASE 1 — 파일 저장 구조 (핵심 선행 작업)

> 현재: 파일 메타데이터만 DB 저장, 실제 파일 없음 → 분석 불가

- [x] 파일 저장 전략 결정 (로컬 vs S3) → **로컬 선택**
  - [x] **로컬 옵션**: `application.yml`에 `upload.path` 설정, 디렉토리 자동 생성
  - [ ] **S3 옵션**: `spring-cloud-aws` 의존성 추가, S3 업로드 서비스 구현
- [x] `UltrasoundImage` 엔티티에 `storedFilePath` 필드 추가
- [x] `PredictionService.analyzeImage()` — 실제 파일 저장 로직 작성
- [x] 저장된 파일 다운로드 / 조회 API 추가 (`GET /api/images/{id}`)
- [x] 파일 확장자 화이트리스트 검증 (`.jpg`, `.png`, `.webp` 등)
- [x] 파일 시그니처(매직 바이트) 검증 추가

---

## PHASE 2 — 인증 개선 (JWT)

> 현재: HTTP Basic Auth (매 요청마다 username/password 전송) → 보안/UX 취약

- [x] `build.gradle`에 `jjwt` 라이브러리 추가 (0.12.6)
- [x] `JwtTokenProvider` 유틸 클래스 작성 (발급 / 검증 / 파싱)
- [x] `JwtAuthenticationFilter` 작성 (`OncePerRequestFilter`)
- [x] `SecurityConfig` 수정 — HTTP Basic 제거, JWT 필터 등록
- [x] 로그인 API 추가 (`POST /api/auth/login` → Access Token 반환)
- [x] Refresh Token 전략 결정 및 구현 (DB 저장)
- [x] `AuthResponse` DTO에 `accessToken` / `tokenType` 필드 추가
- [x] 로그아웃 처리 (`POST /api/auth/logout` → Refresh Token 삭제)

---

## PHASE 3 — 성장 타임라인 (핵심 KPI)

> 유저가 계속 업로드하게 만드는 핵심 기능

### DB 재설계
- [x] `Pregnancy` (임신 정보) 엔티티 추가
  - `id`, `userId`, `dueDate`, `nickname`, `createdAt`
- [x] `UltrasoundImage`에 `pregnancyId`, `weekNumber` (주차) 필드 추가
- [x] `User` ↔ `Pregnancy` (1:N) 관계 설정

### API
- [x] 임신 등록 API (`POST /api/pregnancies`)
- [x] 타임라인 조회 API (`GET /api/pregnancies/{id}/timeline`)
  - 날짜 / 주차별 정렬
  - 분석 결과 포함
- [x] 주차별 아기 표준 정보 응답 포함 (하드코딩, 4~40주)

---

## PHASE 4 — AI 서버 연동

> 현재: `ThreadLocalRandom` 랜덤값 → 실제 Python AI 서버 호출로 교체

### Spring Boot 측
- [x] `OpenAiVisionClient` 구현 (`RestClient` 사용, gpt-4o Vision API)
- [x] API 키 `application.yml`에 외부화 (`openai.api-key`, `openai.model`)
- [x] AI 응답 DTO 정의 (`AiAnalysisResult`)
- [x] `PredictionService.performPrediction()` → `OpenAiVisionClient` 호출로 교체
- [x] AI 서버 타임아웃 / 재시도 설정 (connect: 10s, read: 60s)
- [x] AI 서버 다운 시 Fallback 처리 (시뮬레이션 모드 자동 전환)

### Python AI 서버 (별도 레포)
- [ ] FastAPI 프로젝트 생성
- [ ] `/analyze` 엔드포인트 구현 (이미지 수신 → 모델 추론 → JSON 응답)
- [ ] 모델 선정 및 로드
  - 이미지: YOLO (객체 감지) or Mediapipe
  - 영상: OpenCV + Mediapipe
- [ ] Docker 컨테이너화

---

## PHASE 5 — 비동기 분석 큐

> 영상 처리는 오래 걸림 → 동기 처리 불가

- [x] Redis 의존성 추가 (`spring-boot-starter-data-redis`)
- [x] 분석 요청을 큐에 넣고 즉시 `202 Accepted` 응답
- [x] `analysisStatus` 상태 API (`GET /api/predictions/{id}/status`)
- [x] 분석 완료 시 상태 업데이트 (`PENDING` → `DONE`)
- [ ] (선택) WebSocket or SSE로 실시간 완료 알림

---

## PHASE 6 — GPT 텍스트 리포트 생성

> 숫자/좌표 대신 자연어 리포트로 사용자 경험 개선

- [x] OpenAI API `RestClient`로 직접 호출 (별도 SDK 불필요)
- [x] `OpenAiVisionClient`에 GPT 프롬프트 구성 + 한국어 리포트 생성 통합
- [x] `PredictionResult`에 `reportText` 필드 추가
- [x] 리포트 언어 설정 (한국어 고정)
- [x] GPT API 비용 모니터링 로직 추가

---

## PHASE 7 — SNS 공유 카드

> 바이럴 요소

- [x] 공유 카드 이미지 생성 API (`GET /api/predictions/{id}/share-card`)
  - Java `BufferedImage` or 외부 이미지 생성 서비스 활용
- [ ] OG(Open Graph) 메타태그 포함 공유 URL 생성
- [ ] 카드에 포함할 내용: 날짜, 주차, 리포트 한 줄 요약, 서비스 브랜딩

---

## PHASE 8 — 프론트엔드

> 현재: `index.html` 하나만 존재

- [ ] 프론트엔드 전략 결정
  - [ ] **Thymeleaf 유지**: 서버사이드 렌더링, 빠른 개발
  - [ ] **React/Vue 분리**: API 서버와 완전 분리, SPA
- [ ] 회원가입 / 로그인 페이지
- [ ] 이미지/영상 업로드 페이지
- [ ] 분석 결과 페이지 (리포트 + 공유 버튼)
- [ ] 성장 타임라인 페이지

---

## PHASE 9 — 운영 / 배포

- [x] `Dockerfile` 작성 → `docker/Dockerfile`
- [x] `docker-compose.yml` 작성 → `docker/` (base + dev + test + prod 분리)
- [x] 환경별 `.env` 파일 분리 (`.env.dev`, `.env.test`, `.env.prod.example`)
- [x] 배포 가이드 문서 작성 (`docs/DEPLOY.md`)
- [x] 배포 자동화 스크립트 (`scripts/deploy.sh`)
- [x] `docker/docker-compose.yml`에 Redis 추가
- [x] GitHub Actions CI 파이프라인 구성 (빌드 + 테스트)
- [ ] 서버 배포 (AWS EC2 or Railway 등)
- [ ] HTTPS 설정 (Let's Encrypt)
- [ ] 로그 수집 설정 (Logback → 파일 or CloudWatch)
- [x] 헬스체크 엔드포인트 (`/actuator/health`)

---

## PHASE 10 — Kubernetes

> k8s 숙련자 본인이 직접 작성 예정

- [ ] Namespace 정의 (`aiservice`)
- [ ] `Deployment` — Spring Boot app
- [ ] `Service` — ClusterIP (내부) + LoadBalancer or NodePort (외부)
- [ ] `ConfigMap` — 비민감 환경변수
- [ ] `Secret` — DB 비밀번호, JWT 시크릿, OpenAI API 키
- [ ] MariaDB `StatefulSet` + `PersistentVolumeClaim`
- [ ] `Ingress` — 도메인 라우팅 + TLS
- [ ] `HorizontalPodAutoscaler` — CPU 기준 스케일 아웃
- [ ] `livenessProbe` / `readinessProbe` 설정 (`/actuator/health`)

---

## 테스트 TODO

- [x] `PredictionService` 단위 테스트 — AI 클라이언트 Mock 처리
- [x] `AuthController` 통합 테스트 — 회원가입 / 로그인 플로우
- [x] JWT 필터 테스트
- [ ] 파일 업로드 통합 테스트
- [ ] 타임라인 조회 통합 테스트

---

## 우선순위 요약

| 순서 | PHASE | 이유 |
|---|---|---|
| 1 | PHASE 0 | 코드 안정화, 이후 작업 기반 |
| 2 | PHASE 1 | 파일 저장 없으면 AI 연동 불가 |
| 3 | PHASE 2 | JWT 없으면 프론트 연동 불가 |
| 4 | PHASE 3 | 핵심 KPI (재방문) |
| 5 | PHASE 4 | 실제 서비스 가치 생성 |
| 6 | PHASE 5 | 영상 처리 대응 |
| 7 | PHASE 6 | 사용자 경험 차별화 |
| 8 | PHASE 7 | 바이럴 |
| 9 | PHASE 8 | 프론트 |
| 10 | PHASE 9 | 배포 |
