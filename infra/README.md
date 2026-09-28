# infra — `fungame-infra` 인스턴스 설정

이 폴더는 **`fungame-infra` 인스턴스에만** 배포됩니다. 프로메테우스·그라파나처럼
앱과 별개로 도는 것들이 여기 옵니다.

`.github/workflows/deploy-infra.yml` 이 `infra/**` 변경만 보고 돌며,
이 폴더의 내용을 그대로 `fungame-infra` 의 배포 디렉터리로 밀어넣습니다.

## 여기 오지 않는 것

앱 인스턴스(`fungame`)에서 도는 것은 여기가 아닙니다.

| 파일 | 어디 | 왜 |
| --- | --- | --- |
| `compose.yml` | 저장소 루트 | 앱 인스턴스의 스택 |
| `caddy/Caddyfile` | 저장소 루트 | `reverse_proxy backend:8080` — 앱 compose 네트워크 안에서 돈다 |
| `.env` | 서버에만 | 비밀값. `.env.example` 만 저장소에 둔다 |

앱 쪽은 `deploy-backend.yml` 이 `fungame` 으로 보냅니다. 두 파이프라인은 대상도
시크릿도 다릅니다.

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

## 아직 비어 있습니다

지금 `fungame-infra` 의 설정은 **그 인스턴스에만** 있습니다. 서버에서 가져와 여기 넣어야
변경이 리뷰와 롤백을 타고, 앱 인스턴스를 2대로 늘릴 때 스크랩 타깃 변경도 같이 흐릅니다.

배포 스크립트는 그 서버가 docker compose 로 돈다고 가정합니다. 아니라면
`deploy-infra.yml` 의 마지막 단계를 그에 맞게 바꿔야 합니다.
