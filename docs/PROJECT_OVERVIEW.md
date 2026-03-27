# 프로젝트 개요: 초음파 이미지 태아 성별 예측 서비스

## 기본 정보

| 항목 | 내용 |
|---|---|
| 프레임워크 | Spring Boot 3.4.0 |
| 언어 | Java 21 |
| 빌드 도구 | Gradle |
| 데이터베이스 | MariaDB (포트 33060) |
| 서버 포트 | 8090 |

---

## 프로젝트 목적

초음파 이미지를 업로드하면 AI가 **태아 성별(MALE / FEMALE)을 예측**해주는 REST API 서비스입니다.

> ⚠️ 현재 AI 분석은 실제 모델이 아닌 **랜덤값 시뮬레이션** (신뢰도 60~99%)으로 구현된 골격 코드 상태입니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| 웹 프레임워크 | Spring Boot Web |
| 인증/보안 | Spring Security (HTTP Basic, Stateless) |
| ORM | Spring Data JPA / Hibernate |
| 뷰 | Thymeleaf |
| 비동기 처리 | Java 21 Virtual Threads |
| 비밀번호 암호화 | BCrypt |
| DB 드라이버 | MariaDB JDBC |
| 코드 간소화 | Lombok |
| 유효성 검사 | Spring Validation |

---

## 아키텍처 구조

```
src/main/java/com/example/aiservice/
├── AiServiceApplication.java         # 애플리케이션 진입점
├── config/
│   ├── PasswordEncoderConfig.java    # BCrypt 빈 설정
│   └── SecurityConfig.java           # Spring Security 설정
├── controller/
│   ├── AuthController.java           # 회원가입 API
│   ├── HomeController.java           # 메인 페이지
│   └── PredictionController.java     # 예측 분석 API
├── dto/
│   ├── AuthResponse.java             # 인증 응답 DTO
│   ├── PredictionResponse.java       # 예측 결과 응답 DTO
│   └── SignupRequest.java            # 회원가입 요청 DTO
├── entity/
│   ├── User.java                     # 사용자 엔티티
│   ├── UltrasoundImage.java          # 초음파 이미지 메타데이터 엔티티
│   └── PredictionResult.java         # 예측 결과 엔티티
├── repository/
│   ├── UserRepository.java
│   ├── UltrasoundImageRepository.java
│   └── PredictionResultRepository.java
└── service/
    ├── UserService.java              # 회원가입, UserDetailsService 구현
    └── PredictionService.java        # 이미지 분석 핵심 로직
```

---

## 데이터베이스 엔티티 관계

```
User (1) ──────────── (N) UltrasoundImage (1) ──── (1) PredictionResult
  id                        id                           id
  username                  originalFileName             predictedGender
  password                  contentType                  confidenceScore
  email                     fileSize                     analyzedAt
  role                      uploadedAt
  createdAt
```

---

## API 엔드포인트

### 인증 불필요

| 메서드 | URL | 설명 |
|---|---|---|
| `POST` | `/api/auth/signup` | 회원가입 |
| `GET` | `/` | 메인 페이지 |

### 인증 필요 (HTTP Basic Auth)

| 메서드 | URL | 설명 |
|---|---|---|
| `POST` | `/api/predictions/analyze` | 초음파 이미지 업로드 및 성별 예측 |
| `GET` | `/api/predictions/my-results` | 내 분석 결과 목록 조회 |

---

## 데이터 흐름

```
[클라이언트]
  │
  ├─ POST /api/auth/signup
  │     → 회원가입 (username, password, email)
  │
  └─ POST /api/predictions/analyze (HTTP Basic Auth)
        → 이미지 파일 수신 (multipart/form-data, 최대 10MB)
        → 이미지 메타데이터 DB 저장 (UltrasoundImage)
        → Virtual Thread에서 AI 분석 실행 (현재 3초 딜레이 시뮬레이션)
        → 예측 결과 DB 저장 (PredictionResult)
        → PredictionResponse 반환
              {
                predictionId, imageId, originalFileName,
                predictedGender, confidenceScore,
                analyzedAt, uploadedBy
              }
```

---

## 보안 설정

- **인증 방식**: HTTP Basic Authentication
- **세션 정책**: Stateless (서버에 세션 미저장)
- **CSRF**: 비활성화 (Stateless REST API이므로 불필요)
- **비밀번호**: BCrypt 해시 암호화
- **공개 경로**: `/`, `/api/auth/**`, 정적 리소스, OPTIONS 요청

---

## 구현 현황 및 개선 포인트

### ✅ 구현 완료
- 회원가입 및 인증 (HTTP Basic)
- 초음파 이미지 메타데이터 저장
- Virtual Thread 기반 비동기 AI 분석 처리
- 분석 결과 DB 저장 및 조회

### ⚠️ 미완성 / 개선 필요
| 항목 | 현황 | 개선 방향 |
|---|---|---|
| AI 모델 | 랜덤값 시뮬레이션 | 실제 Python AI 서버(Flask/FastAPI) 연동 |
| 이미지 저장 | 메타데이터만 저장, 파일 미저장 | 파일 시스템 또는 S3에 실제 파일 저장 |
| 인증 방식 | HTTP Basic | JWT 토큰 기반 인증으로 개선 |
| 프론트엔드 | index.html 하나만 존재 | Thymeleaf 페이지 추가 또는 SPA 분리 |
| 파일 유효성 검사 | Content-Type만 확인 | 파일 시그니처(매직 바이트) 검증 추가 |
