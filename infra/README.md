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
| 저장소 루트 `caddy/Caddyfile` | `fungame` | `api.fun-game.club` → `backend:8080` |
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

`INFRA_HOST` 가 비어 있으면 워크플로가 조용히 건너뜁니다. 시크릿을 넣기 전에
머지해도 빨간 X 가 뜨지 않습니다.

## 아직 서버에만 있는 파일

`compose.yml` 이 마운트하는 것 중 둘이 아직 없습니다. 이게 다 들어와야 배포가 실제로 돕니다.

- [x] `prometheus/prometheus.yml`
- [x] `loki/loki-config.yml`
- [ ] `caddy/Caddyfile` — **인프라 쪽 것** (앱 것과 다름)
- [ ] `grafana/provisioning/` — 데이터소스·대시보드 프로비저닝

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
