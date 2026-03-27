#!/bin/bash
# scripts/deploy.sh — 프로덕션 배포 스크립트
# 사용법: ./scripts/deploy.sh [--branch main]
set -e

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BRANCH="${1:-main}"

COMPOSE_CMD="docker compose \
  -f $APP_DIR/docker/docker-compose.base.yml \
  -f $APP_DIR/docker/docker-compose.prod.yml \
  --env-file $APP_DIR/docker/.env.prod"

echo ""
echo "========================================"
echo "  배포 시작 (branch: $BRANCH)"
echo "========================================"

echo ""
echo "==> [1/4] 코드 pull"
cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"
COMMIT=$(git rev-parse --short HEAD)
echo "     커밋: $COMMIT"

echo ""
echo "==> [2/4] 이미지 빌드"
$COMPOSE_CMD build app

echo ""
echo "==> [3/4] 앱 컨테이너 재배포 (DB 무중단)"
$COMPOSE_CMD up -d --no-deps app

echo ""
echo "==> [4/4] 헬스체크 (최대 30초 대기)"
for i in $(seq 1 6); do
  sleep 5
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8090/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{}' || echo "000")
  echo "     시도 $i/6 — HTTP $STATUS"
  if [ "$STATUS" = "400" ] || [ "$STATUS" = "200" ]; then
    echo ""
    echo "✅ 배포 성공! (커밋: $COMMIT)"
    exit 0
  fi
done

echo ""
echo "❌ 헬스체크 실패 — 로그를 확인하세요:"
echo "   docker logs --tail=50 aiservice-app"
exit 1
