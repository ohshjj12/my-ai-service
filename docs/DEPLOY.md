# 배포 가이드

> 마지막 업데이트: 2026-03-27  
> 스택: Spring Boot 3.4 / Java 21 / MariaDB 11.3 / Docker Compose

---

## 목차

1. [사전 준비](#1-사전-준비)
2. [로컬 개발 환경](#2-로컬-개발-환경)
3. [환경별 Docker 실행](#3-환경별-docker-실행)
4. [DB 마이그레이션 & 시드](#4-db-마이그레이션--시드)
5. [GitHub 코드 관리 (브랜치 전략)](#5-github-코드-관리-브랜치-전략)
6. [초기 서버 배포](#6-초기-서버-배포)
7. [업데이트 배포 (무중단)](#7-업데이트-배포-무중단)
8. [롤백](#8-롤백)
9. [헬스체크 & 모니터링](#9-헬스체크--모니터링)
10. [트러블슈팅](#10-트러블슈팅)

---

## 1. 사전 준비

### 로컬 머신
| 항목 | 버전 |
|---|---|
| Java | 21+ |
| Docker Desktop | 25+ |
| Git | 2.40+ |

### 서버 (프로덕션)
```bash
# Docker + Docker Compose plugin 설치 (Ubuntu 22.04 기준)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
sudo apt install -y docker-compose-plugin

# 재로그인 후 확인
docker --version
docker compose version
```

---

## 2. 로컬 개발 환경

### 최초 세팅
```bash
# 1. 저장소 클론
git clone https://github.com/{org}/my-ai.git
cd my-ai

# 2. MariaDB만 Docker로 띄우기 (앱은 IDE에서 실행)
docker compose -f docker/docker-compose.base.yml \
               -f docker/docker-compose.dev.yml \
               --env-file docker/.env.dev \
               up -d db

# 3. DB 접속 확인 (host: 127.0.0.1, port: 33060)
docker exec -it aiservice-db-dev \
  mariadb -u aiservice -pdev_password aiservice -e "SHOW TABLES;"

# 4. 앱 실행 (Gradle)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 전체 스택 (앱 포함)
```bash
docker compose -f docker/docker-compose.base.yml \
               -f docker/docker-compose.dev.yml \
               --env-file docker/.env.dev \
               up -d

# 앱 JVM 디버그 포트: 5005
# API 포트: 8090
```

### 종료
```bash
docker compose -f docker/docker-compose.base.yml \
               -f docker/docker-compose.dev.yml \
               --env-file docker/.env.dev \
               down
```

---

## 3. 환경별 Docker 실행

> 모든 명령어는 **프로젝트 루트**에서 실행합니다.

### 🔵 Dev

```bash
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.dev.yml \
  --env-file docker/.env.dev \
  up -d
```

| | |
|---|---|
| API | `http://localhost:8090` |
| DB  | `localhost:33060` |
| JVM Debug | `localhost:5005` |

---

### 🟡 Test (CI / 통합테스트)

```bash
# 실행 (볼륨 tmpfs → 매번 DB 초기화)
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.test.yml \
  --env-file docker/.env.test \
  up -d

# 테스트 실행
./gradlew test

# 종료 + 볼륨 삭제
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.test.yml \
  --env-file docker/.env.test \
  down -v
```

| | |
|---|---|
| API | `http://localhost:8091` |
| DB  | `localhost:33061` |

---

### 🔴 Prod

```bash
# 1. .env.prod 준비 (최초 1회)
cp docker/.env.prod.example docker/.env.prod
vi docker/.env.prod          # 실제 값 채우기

# 2. 실행
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  up -d

# 3. 로그 확인
docker logs -f aiservice-app
docker logs -f aiservice-db
```

---

## 4. DB 마이그레이션 & 시드

> 현재: JPA `ddl-auto` 로 스키마 자동 관리 (Hibernate DDL)
> 추후: Flyway 도입 예정

### 환경별 DDL 전략

| 환경 | `ddl-auto` | 의미 |
|---|---|---|
| dev | `update` | 변경된 컬럼만 반영 (데이터 유지) |
| test | `create-drop` | 매 실행마다 드롭 후 재생성 |
| prod | `validate` | 스키마 일치 검증만 (불일치 시 기동 실패) |

### 프로덕션 첫 배포 — 스키마 생성

`prod` 환경은 `ddl-auto: validate` 이므로 **첫 배포 시 수동으로 스키마를 생성**해야 합니다.

```bash
# 방법 A: 임시로 update 모드로 한 번 기동
# docker/.env.prod 에 아래 라인 추가 후 실행 → 기동 성공 확인 후 제거
# SPRING_JPA_HIBERNATE_DDL_AUTO=update

# 방법 B: dev 컨테이너에서 스키마 덤프 → prod DB에 import
docker exec -it aiservice-db-dev \
  mariadb-dump -u aiservice -pdev_password \
  --no-data aiservice > docker/schema.sql

# prod DB에 import
docker exec -i aiservice-db \
  mariadb -u aiservice -p${DB_PASSWORD} aiservice < docker/schema.sql
```

### 수동 스키마 확인

```bash
# dev DB 접속
docker exec -it aiservice-db-dev \
  mariadb -u aiservice -pdev_password aiservice

# 테이블 목록
SHOW TABLES;

# 컬럼 확인
DESC ultrasound_images;
DESC prediction_results;
DESC pregnancies;
DESC users;
```

### 시드 데이터 (개발/테스트용)

```bash
# 테스트 유저 생성 (API 이용)
curl -X POST http://localhost:8090/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test1234","email":"test@test.com"}'

# 로그인 → 토큰 획득
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test1234"}'
# 응답의 data.accessToken 값을 TOKEN에 저장

export TOKEN="eyJ..."

# 임신 정보 등록
curl -X POST http://localhost:8090/api/pregnancies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"우리아기","dueDate":"2026-10-01"}'

# 초음파 이미지 업로드 & 분석 (OPENAI_API_KEY 없으면 시뮬레이션)
curl -X POST http://localhost:8090/api/predictions/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/test_image.jpg" \
  -F "pregnancyId=1" \
  -F "weekNumber=16"
```

### SQL 파일로 시드

```sql
-- docker/seed-dev.sql (개발 환경 테스트 데이터)
-- 비밀번호: "test1234" (BCrypt encoded)
INSERT INTO users (username, password, email, role, created_at)
VALUES
  ('admin',    '$2a$10$...', 'admin@test.com',  'ROLE_ADMIN', NOW()),
  ('testuser', '$2a$10$...', 'user@test.com',   'ROLE_USER',  NOW())
ON DUPLICATE KEY UPDATE username = username;
```

```bash
# dev DB에 시드 적용
docker exec -i aiservice-db-dev \
  mariadb -u aiservice -pdev_password aiservice < docker/seed-dev.sql
```

---

## 5. GitHub 코드 관리 (브랜치 전략)

### 브랜치 구조

```
main          ← 프로덕션 배포 기준 (직접 push 금지)
  └── develop ← 통합 개발 브랜치 (PR → main)
        ├── feature/기능명    ← 기능 개발
        ├── fix/버그명        ← 버그 수정
        └── hotfix/이슈명     ← 긴급 수정 (main 직접)
```

### 일반 기능 개발 플로우

```bash
# 1. develop 최신화
git checkout develop
git pull origin develop

# 2. feature 브랜치 생성
git checkout -b feature/pregnancy-timeline

# 3. 개발 → 커밋
git add .
git commit -m "feat: 성장 타임라인 API 추가 (#n)"

# 4. 원격 push
git push origin feature/pregnancy-timeline

# 5. GitHub에서 PR: feature → develop
#    - 리뷰 후 Squash merge
```

### develop → main (릴리즈)

```bash
git checkout main
git pull origin main
git merge --no-ff develop -m "release: v1.0.0"
git tag v1.0.0
git push origin main --tags
```

### hotfix (긴급 수정)

```bash
git checkout main
git checkout -b hotfix/jwt-expiry-bug

# 수정 후
git commit -m "fix: JWT 만료 처리 오류 수정"
git checkout main
git merge --no-ff hotfix/jwt-expiry-bug -m "hotfix: v1.0.1"
git tag v1.0.1
git push origin main --tags

# develop에도 반영
git checkout develop
git merge --no-ff hotfix/jwt-expiry-bug
git push origin develop
```

### 커밋 메시지 컨벤션

| 타입 | 용도 |
|---|---|
| `feat:` | 새 기능 |
| `fix:` | 버그 수정 |
| `refactor:` | 리팩토링 |
| `test:` | 테스트 코드 |
| `docs:` | 문서 |
| `chore:` | 빌드/설정 변경 |
| `hotfix:` | 긴급 수정 |

---

## 6. 초기 서버 배포

### 서버 환경 세팅 (최초 1회)

```bash
# 서버 접속
ssh user@your-server-ip

# 프로젝트 클론
git clone https://github.com/{org}/my-ai.git /opt/my-ai
cd /opt/my-ai

# .env.prod 생성
cp docker/.env.prod.example docker/.env.prod
vi docker/.env.prod
# 아래 항목 필수 입력:
#   DB_ROOT_PASSWORD, DB_PASSWORD
#   JWT_SECRET (openssl rand -base64 64 로 생성)
#   OPENAI_API_KEY

# 업로드 볼륨 디렉토리 권한 확인 (컨테이너가 /data/uploads 사용)
# Docker volume이 자동 관리하므로 별도 작업 불필요
```

### 첫 배포 실행

```bash
cd /opt/my-ai

# 1. 이미지 빌드 + 컨테이너 기동
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  up -d --build

# 2. 스키마 생성 확인 (ddl-auto: validate 전 update로 1회)
# docker/.env.prod 에 아래 추가 후 재기동
#   SPRING_JPA_HIBERNATE_DDL_AUTO=update
docker compose ... up -d --build

# 스키마 생성 확인 후 .env.prod에서 위 줄 제거 → 재기동
docker compose ... up -d

# 3. 동작 확인
curl http://localhost:8090/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234!","email":"admin@domain.com"}'
```

---

## 7. 업데이트 배포 (무중단)

### ⚠️ Docker Compose의 한계

`docker compose up -d --no-deps app` 은 **컨테이너를 Stop → Start** 하는 방식이라  
교체 순간 ~수 초의 다운타임이 발생합니다.  
Java/Spring은 JVM warm-up + DB 커넥션풀 초기화까지 있어서 실제 서비스 준비까지 **10~30초** 소요됩니다.

> **진짜 무중단은 K8s RollingUpdate + readinessProbe 조합** → [섹션 7-2](#7-2-k8s-rollingupdate-권장) 참고

---

### 7-1. Docker Compose 배포 (다운타임 감수 or 트래픽 적을 때)

```bash
cd /opt/my-ai

# 1. 최신 코드 pull
git pull origin main

# 2. 이미지 빌드 (앱만)
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  build app

# 3. 앱 컨테이너만 재시작 (DB 중단 없음, 단 앱은 수 초 다운)
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  up -d --no-deps app

# 4. Graceful Shutdown 확인 (진행 중 요청 완료 후 종료)
docker logs --tail=30 aiservice-app | grep -E "Graceful|Started|shutdown"
```

> **Graceful Shutdown 동작 원리**  
> SIGTERM 수신 → 새 요청 거부 → 진행 중 요청 최대 30초 대기 완료 → 종료  
> `application.yml`의 `server.shutdown: graceful` + `lifecycle.timeout-per-shutdown-phase: 30s` 설정 적용됨

---

### 7-2. K8s RollingUpdate (권장)

K8s는 `readinessProbe`가 OK를 반환한 새 Pod이 준비된 후에 구 Pod을 제거합니다.  
Spring Actuator의 `/actuator/health/readiness` 엔드포인트가 이 역할을 합니다.

```
구 Pod (v1) ─────── 트래픽 수신 중
새 Pod (v2) ─── 기동 중 → readinessProbe Fail → 트래픽 없음
                      ↓ DB 연결 + 앱 준비 완료
                      readinessProbe OK → 트래픽 수신 시작
                                              ↓
구 Pod (v1) ─────── SIGTERM → Graceful Shutdown → 종료
```

#### K8s Deployment 핵심 설정 (예시)

```yaml
# k8s/deployment.yaml (추후 PHASE 10 에서 작성 예정)
spec:
  replicas: 2                      # 최소 2개로 롤링 가능
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1                  # 최대 1개 추가 기동
      maxUnavailable: 0            # 항상 replicas 수 유지 (다운타임 0)
  template:
    spec:
      containers:
        - name: app
          image: my-ai:latest
          ports:
            - containerPort: 8090

          # Spring Actuator probe 엔드포인트 활용
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8090
            initialDelaySeconds: 20   # JVM warm-up 대기
            periodSeconds: 5
            failureThreshold: 6       # 30초 동안 실패하면 비정상 처리

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8090
            initialDelaySeconds: 40
            periodSeconds: 10
            failureThreshold: 3

          # Graceful Shutdown: K8s가 SIGTERM 보낸 후 terminationGracePeriod 동안 대기
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 5"]  # LB에서 Pod 제거 전 5초 버퍼
      terminationGracePeriodSeconds: 40  # Spring shutdown 30s + 버퍼 10s
```

#### 배포 명령

```bash
# 이미지 빌드 & 푸시
docker build -f docker/Dockerfile -t my-ai:v1.1.0 .
docker tag my-ai:v1.1.0 {registry}/my-ai:v1.1.0
docker push {registry}/my-ai:v1.1.0

# 롤링 업데이트
kubectl set image deployment/aiservice-app app={registry}/my-ai:v1.1.0 -n aiservice

# 진행 상황 확인
kubectl rollout status deployment/aiservice-app -n aiservice

# 완료 후 검증
kubectl get pods -n aiservice
```

#### HPA (Auto Scaling)

```bash
# CPU 50% 초과 시 최대 5개로 스케일 아웃
kubectl autoscale deployment aiservice-app \
  --cpu-percent=50 \
  --min=2 \
  --max=5 \
  -n aiservice

kubectl get hpa -n aiservice
```

---

### 스크립트로 자동화 (Docker Compose)

```bash
# /opt/my-ai/scripts/deploy.sh
./scripts/deploy.sh [--branch main]
```

---

## 8. 롤백

### 이전 이미지로 롤백

```bash
# 현재 실행 중인 이미지 확인
docker ps --format "table {{.Image}}\t{{.Names}}\t{{.Status}}"

# 이전 태그로 코드 되돌리기
cd /opt/my-ai
git log --oneline -10            # 커밋 히스토리 확인
git checkout v1.0.0              # 또는 git reset --hard <commit>

# 이전 버전으로 빌드 & 재배포
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  up -d --build --no-deps app

echo "✅ 롤백 완료"
```

---

## 9. 헬스체크 & 모니터링

### 로그 확인

```bash
# 실시간 로그
docker logs -f aiservice-app
docker logs -f aiservice-db

# 최근 100줄
docker logs --tail=100 aiservice-app

# 에러 필터링
docker logs aiservice-app 2>&1 | grep -i error
```

### 컨테이너 상태

```bash
docker ps -a
docker stats aiservice-app aiservice-db
```

### DB 상태 확인

```bash
# prod DB 접속
docker exec -it aiservice-db \
  mariadb -u aiservice -p${DB_PASSWORD} aiservice

# 최근 분석 결과 확인
SELECT id, predicted_gender, analysis_status, analyzed_at
FROM prediction_results
ORDER BY analyzed_at DESC LIMIT 10;

# 유저 수 확인
SELECT COUNT(*) FROM users;
```

### 볼륨 용량 확인

```bash
docker system df -v
du -sh /var/lib/docker/volumes/my-ai_uploads
du -sh /var/lib/docker/volumes/my-ai_db-data
```

---

## 10. 트러블슈팅

### 앱이 기동되지 않을 때

```bash
# 로그에서 원인 확인
docker logs aiservice-app 2>&1 | tail -50

# 자주 발생하는 원인
# 1. DB_URL, DB_PASSWORD 오타 → .env.prod 확인
# 2. prod에서 ddl-auto=validate + 스키마 불일치 → validate → update 1회 실행
# 3. JWT_SECRET 미설정 → .env.prod에 JWT_SECRET 추가
# 4. 포트 충돌 → lsof -i :8090
```

### DB 연결 실패

```bash
# DB 컨테이너 상태 확인
docker inspect aiservice-db | grep -A5 Health

# 수동 헬스체크
docker exec aiservice-db \
  mariadb -u aiservice -p${DB_PASSWORD} -e "SELECT 1;"

# 네트워크 확인 (app → db 통신)
docker network ls
docker network inspect my-ai_default
```

### 파일 업로드 실패

```bash
# 업로드 볼륨 마운트 확인
docker exec aiservice-app ls -la /data/uploads

# 디스크 용량 확인
df -h
```

### 이미지 빌드 실패

```bash
# 캐시 없이 클린 빌드
docker compose \
  -f docker/docker-compose.base.yml \
  -f docker/docker-compose.prod.yml \
  --env-file docker/.env.prod \
  build --no-cache app

# 불필요한 이미지/컨테이너 정리
docker system prune -f
```
