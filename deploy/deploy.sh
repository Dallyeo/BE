#!/usr/bin/env bash
#
# 블루-그린 무중단 배포 (단일 EC2, 서버에서 실행).
# GitHub Actions 가 새 JAR 을 scp 로 올린 뒤 이 스크립트를 SSH 로 실행한다.
#
# 사용: sudo ./deploy.sh /opt/dallyeo/releases/app-<sha>.jar
#
# 동작: 놀고있는 포트로 새 버전 기동 → /actuator/health 대기 → Nginx upstream 스위치 + reload
#       → 옛 포트 드레인 후 종료. 헬스 실패 시 스위치 없이 새 인스턴스만 내림(안전 롤백).
set -euo pipefail

NEW_JAR="${1:?새 JAR 경로를 인자로 주세요}"
UPSTREAM_CONF="/etc/nginx/conf.d/dallyeo-upstream.conf"
APP_DIR="/opt/dallyeo"
HEALTH_TIMEOUT=60   # 초
DRAIN_SECONDS=15

[[ -f "$NEW_JAR" ]] || { echo "JAR 없음: $NEW_JAR"; exit 1; }

# 현재 활성 포트 파악(upstream conf 에서), 유휴 포트 계산
CURRENT_PORT="$(grep -oE '127\.0\.0\.1:[0-9]+' "$UPSTREAM_CONF" | grep -oE '[0-9]+$' || echo 8080)"
if [[ "$CURRENT_PORT" == "8080" ]]; then IDLE_PORT=8081; else IDLE_PORT=8080; fi
echo "▶ 현재 활성 포트=$CURRENT_PORT / 새 버전 배포 포트=$IDLE_PORT"

# 유휴 포트가 새 JAR 을 실행하도록 심볼릭 링크 지정 후 기동
ln -sfn "$NEW_JAR" "$APP_DIR/app-$IDLE_PORT.jar"
echo "▶ dallyeo@$IDLE_PORT 기동..."
systemctl restart "dallyeo@$IDLE_PORT"

# 헬스체크 대기
echo "▶ 헬스체크(/actuator/health) 대기..."
deadline=$(( SECONDS + HEALTH_TIMEOUT ))
until curl -fs "http://127.0.0.1:$IDLE_PORT/actuator/health" | grep -q '"status":"UP"'; do
    if (( SECONDS >= deadline )); then
        echo "✖ 헬스체크 실패 — 롤백(새 인스턴스 종료, 기존 $CURRENT_PORT 유지)"
        systemctl stop "dallyeo@$IDLE_PORT" || true
        exit 1
    fi
    sleep 2
done
echo "✔ $IDLE_PORT UP"

# Nginx upstream 을 새 포트로 스위치 + 검증 후 reload(무중단)
echo "▶ Nginx 트래픽 전환 $CURRENT_PORT → $IDLE_PORT"
cat > "$UPSTREAM_CONF" <<EOF
upstream dallyeo_backend {
    server 127.0.0.1:$IDLE_PORT;
}
EOF
nginx -t
systemctl reload nginx

# 옛 인스턴스 드레인 후 종료
echo "▶ 기존 인스턴스 드레인 ${DRAIN_SECONDS}s 후 종료..."
sleep "$DRAIN_SECONDS"
systemctl stop "dallyeo@$CURRENT_PORT" || true

echo "✅ 배포 완료 — 활성 포트=$IDLE_PORT"
