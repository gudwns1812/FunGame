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
| `INFRA_PROJECT_DIR` | 서버의 배포 디렉터리 | `/home/ubuntu/fungame-infra` |

`INFRA_HOST` 가 비어 있으면 워크플로가 조용히 건너뜁니다. 시크릿을 넣기 전에
머지해도 빨간 X 가 뜨지 않습니다.

## 아직 서버에만 있는 파일

`compose.yml` 은 가져왔지만 그것이 마운트하는 파일들은 아직 없습니다.
이게 다 들어와야 배포가 실제로 돕니다.

- [ ] `loki/loki-config.yml`
- [ ] `prometheus/prometheus.yml`
- [ ] `caddy/Caddyfile` — **인프라 쪽 것** (앱 것과 다름)
- [ ] `grafana/provisioning/` — 데이터소스·대시보드 프로비저닝

## 정리하면서 확인할 것 두 가지

**1. 프로메테우스 설정만 절대 경로입니다.**

```yaml
- /opt/monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
```

나머지 셋은 `./loki/...`, `./grafana/...`, `./caddy/...` 로 compose 파일 기준 상대 경로인데
이것만 절대 경로입니다. 배포 디렉터리가 `/opt/monitoring` 이라면 가리키는 곳은 같지만,
`scp` 가 파일을 배포 디렉터리로 밀어넣는 구조에서는 **두 경로가 갈라지는 순간 조용히
옛 설정으로 뜹니다.** `./prometheus/prometheus.yml` 로 맞추는 편이 안전합니다.

**2. Loki 3100 포트가 공개돼 있습니다.**

```yaml
ports:
  - "3100:3100"
```

주소를 지정하지 않아 `0.0.0.0` 에 열립니다. Loki 는 기본적으로 인증이 없어서, 보안 그룹이
유일한 방어선입니다. 앱 인스턴스가 로그를 밀어 넣어야 해서 열어둔 것이라면 앱 쪽
`MANAGEMENT_BIND_IP` 처럼 **사설 IP 에 묶는 것**이 맞습니다.

프로메테우스와 그라파나는 포트를 공개하지 않고 Caddy 뒤에 있습니다. Loki 만 예외입니다.
