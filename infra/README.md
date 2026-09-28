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

## 대시보드는 저장소에서 만듭니다

그라파나가 프로비저닝 폴더를 `updateIntervalSeconds`(30초)마다 다시 읽고, compose 가 그 폴더를
바인드 마운트합니다. 그래서 **JSON 을 저장소에 넣고 배포하면 재시작 없이 반영됩니다.**

```
저장소에 JSON 추가 → 머지 → deploy-infra 가 scp → 30초 안에 대시보드 등장
```

프로바이더 하나가 폴더 하나를 맡습니다. 폴더를 나누려면 `dashboards.yml` 에 블록을 더하세요.

| 프로바이더 | 폴더 | 경로 |
| --- | --- | --- |
| Application Logs | `Logs` | `dashboards/json/` |
| Metrics | `Metrics` | `dashboards/json-metrics/` |

`allowUiUpdates: true` 라 UI 에서도 고칠 수 있습니다. 편하지만 파일과 갈라집니다 — 파일이
바뀌면 파일이 이기고, 안 바뀌면 UI 쪽이 남습니다. **UI 수정은 임시**로 생각하고, 남길 것은
파일로 옮기세요. 저장소만 진실로 삼고 싶으면 `false` 로 내리면 됩니다.

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
