# 배포 가이드 — 단일 EC2 블루-그린 + Let's Encrypt + GitHub Actions

전제(완료): AWS EC2 에 Java17 · MySQL · Redis · Nginx 설치됨. 도메인이 EC2 를 가리킴.

구성 요약:
- 앱 인스턴스 2개(포트 **8080/8081**)를 systemd 템플릿으로 운영, Nginx 가 활성 포트로 프록시.
- `main` push → GitHub Actions 빌드·테스트 → EC2 로 JAR scp → `deploy.sh` 가 유휴 포트 기동·헬스체크·트래픽 전환·옛 인스턴스 종료.
- DB/Redis 는 공유(한 개). 시크릿은 서버 `/etc/dallyeo/dallyeo.env` 에만.

---

## 1) 서버 최초 세팅 (1회)

```bash
# 앱 사용자/디렉토리
sudo useradd -r -s /usr/sbin/nologin dallyeo || true
sudo mkdir -p /opt/dallyeo/releases /etc/dallyeo
sudo chown -R dallyeo:dallyeo /opt/dallyeo

# 시크릿 파일(권한 600) — 실제 값 채우기
sudo install -m 600 deploy/dallyeo.env.example /etc/dallyeo/dallyeo.env
sudo vi /etc/dallyeo/dallyeo.env        # DB_USERNAME/DB_PASSWORD/TOURAPI_SERVICE_KEY/JWT_SECRET

# MySQL: DB + 계정 (dallyeo.env 값과 일치시킬 것)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS dallyeo CHARACTER SET utf8mb4;
  CREATE USER IF NOT EXISTS 'dallyeo'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';
  GRANT ALL PRIVILEGES ON dallyeo.* TO 'dallyeo'@'localhost'; FLUSH PRIVILEGES;"

# systemd 템플릿
sudo cp deploy/dallyeo@.service /etc/systemd/system/dallyeo@.service
sudo systemctl daemon-reload

# Nginx (도메인 교체 후)
sudo cp deploy/nginx/dallyeo-upstream.conf /etc/nginx/conf.d/
sudo cp deploy/nginx/dallyeo.conf          /etc/nginx/conf.d/
#sudo sed -i 's/your-domain.com/실제도메인/' /etc/nginx/conf.d/dallyeo.conf
sudo nginx -t && sudo systemctl reload nginx

# deploy.sh 배치 + 실행권한
sudo cp deploy/deploy.sh /opt/dallyeo/deploy.sh
sudo chmod +x /opt/dallyeo/deploy.sh
```

### 배포 사용자 sudo 권한 (GitHub Actions SSH 계정)
`deploy.sh` 는 systemctl/nginx 제어에 sudo 가 필요합니다. SSH 로 접속하는 계정에 무암호 sudo 부여:
```bash
echo '<SSH_USER> ALL=(root) NOPASSWD: /opt/dallyeo/deploy.sh, /usr/bin/install' | sudo tee /etc/sudoers.d/dallyeo
```

## 2) SSL (Let's Encrypt)
```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 실제도메인          # 443 + 자동 리다이렉트 + 인증서 설정 자동
# 자동 갱신 확인(certbot 이 systemd timer 등록): sudo certbot renew --dry-run
```

## 3) 최초 1회 수동 배포 (8080 부팅)
GitHub Actions 전에 한 번 손으로 띄워 활성 상태를 만듭니다.
```bash
# 빌드된 JAR 을 서버에 두고(예: 로컬에서 scp), 첫 인스턴스 기동
sudo ln -sfn /opt/dallyeo/releases/<첫JAR>.jar /opt/dallyeo/app-8080.jar
sudo systemctl enable --now dallyeo@8080
curl -s localhost:8080/actuator/health      # {"status":"UP"} 확인
```
이후부터는 GitHub Actions 가 8081↔8080 을 오가며 자동 배포합니다.

## 4) GitHub 저장소 Secrets 등록
`Settings → Secrets and variables → Actions`:
| Secret | 값 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | SSH 접속 계정 |
| `EC2_SSH_KEY` | 그 계정의 **개인키(PEM 전체)** |
| `EC2_SSH_PORT` | (선택) SSH 포트, 기본 22 |

> ⚠️ EC2 보안그룹에서 SSH(22) 인바운드를 GitHub 러너에 허용해야 합니다. 러너 IP 는 광범위하므로, 보안이 중요하면 self-hosted 러너로 전환 권장(그 경우 인바운드 SSH 불필요).

## 5) 동작 / 롤백
- `main` push → Actions 자동 실행. 유휴 포트에서 `/actuator/health` 가 UP 이 될 때만 트래픽 전환.
- **헬스 실패 시**: 트래픽 전환하지 않고 새 인스턴스만 내림 → **기존 버전이 계속 서빙**(자동 안전 롤백).
- 수동 롤백: 직전 릴리스 JAR 로 `sudo /opt/dallyeo/deploy.sh /opt/dallyeo/releases/<이전>.jar`.

## 스키마 마이그레이션 (`ddl-auto=update` 가 못 하는 변경)
`ddl-auto=update` 는 **테이블·컬럼 추가만** 합니다. 기존 컬럼의 타입/길이 변경은 하지 않으므로,
그런 변경이 필요한 배포는 **코드 배포 전에 SQL 을 손으로 적용**해야 합니다.

```bash
ssh <EC2_USER>@<EC2_HOST>

# 1) 백업 (해당 테이블만)
sudo mysqldump -u root -p dallyeo user_achievement \
  > ~/user_achievement.$(date +%Y%m%d%H%M).sql

# 2) 적용 — 로컬 레포의 SQL 을 붙여넣거나 scp 로 올려서 실행
sudo mysql -u root -p dallyeo < migrate-user-achievement-varchar.sql

# 3) 확인
sudo mysql -u root -p dallyeo -e "SHOW COLUMNS FROM user_achievement LIKE 'achievement'"
```

**순서**: 마이그레이션 → 그다음 `main` push(자동 배포). 반대로 하면 그 사이에 들어온 요청이 실패합니다.

**적용 이력**
| 날짜 | 파일 | 내용 | 이유 |
|---|---|---|---|
| 2026-09-13 | `migrate-user-achievement-varchar.sql` | `user_achievement.achievement` enum → `VARCHAR(40)` | 업적 8종→21종 확장. Hibernate 가 만든 네이티브 enum 컬럼이 옛 8종만 허용해 신규 업적 저장이 500 으로 실패 |

## 주의
- `spring.jpa.hibernate.ddl-auto=update`: 블루·그린이 같은 스키마 공유 → 컬럼 삭제/변경형 배포는 위험. 추가형만 안전. 운영 안정화 시 Flyway 전환 권장.
- 응답 구조가 바뀌는 배포에서는 Redis 캐시 키 버저닝 또는 flush 고려.
