# infra — `fungame-infra` 인스턴스 설정

이 폴더는 **`fungame-infra` 인스턴스에만** 배포됩니다. 프로메테우스·그라파나처럼
앱과 별개로 도는 것들이 여기 옵니다.

`.github/workflows/deploy-infra.yml` 이 `infra/**` 변경만 보고 돌며,
이 폴더의 내용을 그대로 `fungame-infra` 의 배포 디렉터리로 밀어넣습니다.

## 스택

`loki` · `prometheus` · `grafana` · `caddy` 네 개가 `monitoring` 네트워크에 묶여 있습니다.

## Caddy 가 양쪽에 있습니다

**`fungame` 과 `fungame-infra` 가 각자 Caddy 를 갖고 있습니다. 서로 다른 파일입니다.**

| Caddyfile | 인스턴스 | 무엇을 |
| --- | --- | --- |
| `backend/caddy/Caddyfile` | `fungame` | `api.fun-game.club` → `backend:8080` |
| `infra/caddy/Caddyfile` | `fungame-infra` | `grafana.fun-game.club` → 그라파나 |

이름이 같아 헷갈리기 쉽습니다. 고치기 전에 **어느 인스턴스의 것인지** 먼저 확인하세요.
앱 쪽은 `deploy-backend.yml` 이 `fungame` 으로, 이 폴더는 `deploy-infra.yml` 이
`fungame-infra` 로 보냅니다. 대상도 시크릿도 겹치지 않습니다.

`.env` 도 인스턴스마다 따로입니다. 여기 것은 `infra/.env.example` 을 보세요.

## 필요한 시크릿

`deploy-backend.yml` 이 쓰는 `EC2_*` 와 **별개**입니다. 대상 호스트가 다르므로 섞지 않습니다.

| 시크릿 | 설명 | 기본값 |
| --- | --- | --- |
| `INFRA_HOST` | `fungame-infra` 주소 | 없음 (없으면 워크플로를 건너뜁니다) |
| `INFRA_USER` | SSH 계정 | 없음 |
| `INFRA_SSH_KEY` | SSH 개인키 | 없음 |
| `INFRA_PORT` | SSH 포트 | `22` |
| `INFRA_PROJECT_DIR` | 서버의 배포 디렉터리 | `/opt/monitoring` |
| `DISCORD_ALERT_WEBHOOK_URL` | 그라파나 알림을 보낼 디스코드 웹훅. 배포가 서버 `.env` 에 넣는다 | 없음 |

### 배포 디렉터리 소유권

`INFRA_PROJECT_DIR`(기본 `/opt/monitoring`)과 그 아래 파일을 **`INFRA_USER` 가 소유해야** 합니다.
`/opt` 는 보통 root 소유라 그대로 두면 scp 가 `Cannot utime: Operation not permitted` 로 죽습니다.

```
sudo chown -R ubuntu:ubuntu /opt/monitoring
```

`INFRA_HOST` · `INFRA_USER` · `INFRA_SSH_KEY` **셋이 다 있어야** 배포가 돕니다. 하나라도
없으면 워크플로가 건너뛰고 성공으로 끝납니다. 어느 것이 없는지는 실행 요약에 적힙니다.

셋 중 일부만 넣어두면 건너뛰므로, 넣었는데 배포가 안 돈다면 요약부터 보세요.

## 대시보드는 저장소에서 만듭니다

대시보드는 `grafana/provisioning/dashboards/fungame/` 의 JSON 이고 그라파나 `FunGame` 폴더 하나에 뜹니다.
**서비스 개요**에서 정상 여부를 보고, 위쪽 링크로 상세 대시보드에 내려갑니다.

| 대시보드 | 답하는 질문 |
| --- | --- |
| 서비스 개요 | 지금 정상인가? 받는 인스턴스 수 · 처리량 · 5xx · 응답시간 · ERROR 로그 · 가동 시간 |
| API | 어느 엔드포인트가 느리거나 실패하나? |
| 실시간 · 게임 | 방 · 게임 · STOMP · 라운드 타이머 · 브로드캐스트 전파 |
| 의존성 | MySQL 커넥션 풀 · Redis 명령 · 서킷 브레이커 |
| 인프라 | JVM · 컨테이너 메모리 · 재시작 · 앱 서버 호스트 자원 |
| 사용자 | 로그인 · DAU |
| 로그 | 인스턴스 · 레벨 · 검색어로 로그 보기 |

방 · 게임 · 방 안 인원 · 접속 회원은 모든 인스턴스가 같은 전체 값을 보고하므로 `max` 로 봅니다.
`sum` 하면 인스턴스 수만큼 부풀어 보입니다.

그라파나는 JSON 내용은 30초마다 다시 읽지만 **프로바이더 목록(`dashboards.yml`)은 기동할 때만** 읽습니다.
그래서 `deploy-infra` 가 매 배포마다 그라파나를 다시 띄웁니다.

`allowUiUpdates: true` 라 UI 에서도 고칠 수 있습니다. 편하지만 파일과 갈라집니다 — 파일이
바뀌면 파일이 이기고, 안 바뀌면 UI 쪽이 남습니다. **UI 수정은 임시**로 생각하고, 남길 것은
파일로 옮기세요.

## 알림

`grafana/provisioning/alerting/` 의 규칙이 디스코드 알림 채널로 갑니다. 규칙 폴더도 `FunGame` 입니다.

| 알림 | 조건 | 등급 |
| --- | --- | --- |
| 서비스 중단 | 트래픽을 받는 인스턴스 0대가 1분 | critical |
| DB 커넥션 대기 | Hikari 대기 > 0 이 2분 | critical |
| 5xx 급증 | 5xx 비율 > 5% 가 5분 (초당 0.1 요청 미만이면 보지 않는다) | critical |
| 한 대로만 운영 중 | 받는 인스턴스 1대가 10분 | warning |
| 응답 지연 | p95 > 1초가 10분 | warning |
| ERROR 로그 급증 | 5분간 ERROR > 20건 | warning |
| 로그 수집 끊김 | Loki 에 백엔드 로그가 10분간 0건 | warning |
| 앱 서버 디스크 · 메모리 | 디스크 > 85%, 사용 가능 메모리 < 10% | warning |

- 웹훅 주소는 GitHub Secret `DISCORD_ALERT_WEBHOOK_URL` 이고 `deploy-infra` 가 서버 `.env` 에 넣습니다.
  신고 웹훅과 다른 채널입니다. 신고에는 정답과 힌트가 실립니다.
- 같은 알림은 묶어 보내고 울리는 중이면 4시간마다 다시 알립니다. 해결되면 해결 메시지도 갑니다.
- 인스턴스 재시작 알림은 두지 않습니다. 배포마다 재시작해서 혼자 재시작한 것과 가르기 어렵습니다.
  재시작은 인프라 대시보드의 "24시간 동안 기동 횟수" 로 봅니다.
- 프로비저닝 파일의 `$NAME` 은 환경 변수로 치환됩니다. 규칙 문구에 `$` 를 쓰려면 `$$` 로 적습니다.

## 데이터소스

| 이름 | uid | 비고 |
| --- | --- | --- |
| Loki | `loki` | 기본 데이터소스 |
| prometheus | `afvghsyndws8we` | UI 에서 만들었던 것을 프로비저닝이 인수합니다 |

프로메테우스 uid 가 랜덤 문자열인 것은 UI 에서 먼저 만들었기 때문입니다. 새 uid 를 주면
데이터소스가 둘로 보이고 이 uid 를 박아 쓰던 대시보드가 깨지므로 그대로 물려받습니다.
프로비저닝된 데이터소스는 UI 에서 읽기 전용이 되고, 이제 이 파일이 진실입니다.

## 스크랩 대상

프로메테우스가 앱 인스턴스의 사설 IP 를 직접 긁습니다.

| job | 대상 | 나오는 곳 |
| --- | --- | --- |
| `fungame-backend` | `:8081/actuator/prometheus` | 앱 compose 의 `MANAGEMENT_BIND_IP` 가 묶는 포트 |
| `node` | `:9100` | node_exporter. **앱 compose 에 없습니다** — 그 호스트에 따로 떠 있습니다 |

타깃마다 `instance: app-1` 라벨이 붙어 있습니다. 인스턴스를 2대로 늘릴 때
`app-2` 블록을 여기 더하면 되고, 인프라 설정이 이 저장소에 있으니 **앱 변경과 같은 PR 로 흐릅니다.**

node_exporter 가 compose 밖에 있는 것은 따로 챙겨야 할 부분입니다. 앱 서버를 새로 세우면
그것도 같이 올려야 하는데 그 절차가 어디에도 적혀 있지 않습니다.

## Loki 3100 이 공개돼 있습니다

```yaml
ports:
  - "3100:3100"
```

주소를 지정하지 않아 `0.0.0.0` 에 열리고, `loki-config.yml` 은 `auth_enabled: false` 입니다.
**인증이 없는 로그 저장소가 인터넷에 열려 있고 보안 그룹이 유일한 방어선입니다.**
누구나 로그를 읽고 쓸 수 있습니다.

프로메테우스와 그라파나는 포트를 공개하지 않고 Caddy 뒤에 있습니다. Loki 만 예외입니다.
앱 인스턴스가 로그를 밀어 넣어야 해서 열어둔 것이라면, 앱 쪽 `MANAGEMENT_BIND_IP` 처럼
사설 IP 에 묶는 것이 맞습니다.

```yaml
- "${LOKI_BIND_IP:-0.0.0.0}:3100:3100"
```

기본값을 지금 동작 그대로 두면 `.env` 한 줄로 닫을 수 있습니다. 아직 바꾸지 않았습니다 —
로그를 밀어 넣는 쪽이 공인 IP 로 붙고 있으면 끊기기 때문입니다. 무엇이 쓰는지 확인이 필요합니다.
