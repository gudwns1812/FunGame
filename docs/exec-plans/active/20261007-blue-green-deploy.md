# 실행 계획: 배포 중에도 판이 끊기지 않게 한다

작성 2026-10-07 · 개정 2026-10-08
대상: `backend/compose.yml` · `backend/caddy/Caddyfile` · `backend/Dockerfile` · `.github/workflows/deploy-backend.yml` · `infra/prometheus/`

[20260928-scale-out-multi-instance.md](20260928-scale-out-multi-instance.md) 의 5단계를 푼다.
끝나면 이 문서와 5단계 항목을 함께 지운다. **남은 일만 적는다.**

**평시 2대로 간다.** MySQL 을 전용 호스트로 뺀 자리(#105)에 JVM 을 하나 더 올린다.
**앞단은 Caddy 다** — ALB 를 쓰지 않고 호스트도 늘리지 않는다 (상위 §1.11).

---

## 1. 번복하지 않을 것

**전환은 Caddy 가 readiness 를 보고 한다.** 배포 스크립트가 Caddyfile 을 고쳐 쓰지 않는다.
전환은 `/actuator/traffic` 호출 한 번이고, readiness 가 503 이 되면 Caddy 가 뺀다.

```
reverse_proxy backend-blue:8080 backend-green:8080 {
	lb_policy cookie
	health_uri /actuator/health/readiness
	health_port 8081
	health_interval 2s
	health_timeout 1s
}
```

`fail_duration` 은 패시브 옵션이라 넣지 않는다. 빠진 이유가 둘이 되면 전환이 왜 안 됐는지 못 가린다.
대신 **`lb_try_duration` 으로 다시 보낸다.** 헬스체크가 알아차리기 전에 한쪽이 죽으면 그 사이 요청이
502 가 된다 — 첫 배포 리허설에서 실제로 한 번 났다. 재시도를 넣으니 0 이 됐다.

**드레인은 기다리지 않고 끊는다.** 판이 도는 방의 WebSocket 은 몇 분씩 살아 기다리면 영영 안 빠진다.
readiness 를 내려 새 연결을 막고 graceful shutdown 으로 끊는다. 프런트가 다시 붙고 판은 Redis 에 있어
`rejoinPlayingRoom` 이 잇는다.

**배포 한 번에 모든 접속이 최소 한 번 끊긴다.** 한쪽을 뺀 순간 쿠키 스티키가 남은 쪽으로 다시 붙이므로,
두 번째 인스턴스를 올릴 때는 전원이 거기 몰려 있다. 로컬 실측으로 **끊김은 한 사람당 1~2회, 복구는
550~680ms** 였다. 이것을 줄이는 길은 드레인을 기다리는 것뿐이고, 그러면 그 인스턴스가 안 빠진다.

**Flyway 에 장치를 달지 않는다.** `GET_LOCK` 이 있고 두 쪽이 시차를 두고 뜬다. 대신 **스키마는 확장 후
축소**다(상위 §1.9) — 롤백이 "v2 가 쓴 것을 v1 이 읽기" 를 강제한다. 컬럼을 지우거나 이름을 바꾸는
마이그레이션 한 번이면 아직 도는 옛 인스턴스가 깨진다.

---

## 2. 작업

### B0 — 메모리

| | |
|---|---|
| B0-1 | `Dockerfile` 에 `-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=128m` |
| B0-2 | `docker stats --no-stream` 으로 JVM 하나의 RSS 를 재고 2개가 들어가는지 본다 |

RSS 는 힙 + 메타스페이스 + 코드캐시 + GC 구조 + 스레드 스택이라 힙 256MB 라도 500MB 가까이 쓴다.

**2026-10-08 실측 — 2GB 에 두 대가 넉넉히 들어간다.**

| `mem_limit` | blue | green | 합계 |
|---|---|---|---|
| 700m | 415MiB | 454MiB | **869MiB** |
| 420m | 412MiB (98%) | 404MiB (96%) | 816MiB |

Caddy 17MiB 와 OS 를 더해도 **2GB 의 절반을 조금 넘는 수준**이라 평시 2대로 충분하다.

420m 줄이 말하는 것은 따로다 — **한도를 조이면 JVM 이 그 한도에 붙는다.** `MaxRAMPercentage=50`
이라 힙이 한도를 따라 줄고, 그만큼 GC 가 잦아진다. 총량을 줄이려고 한도를 깎는 것은 득이 없다.
`mem_limit` 은 **폭주를 막는 상한**이지 사용량을 정하는 값이 아니다.

### B1 — 컨테이너를 둘로

| | |
|---|---|
| B1-1 | `backend` → `backend-blue` · `backend-green`. `container_name` 도 함께 |
| B1-2 | `caddy` 의 `depends_on: backend` 를 두 서비스로. 그대로 두면 compose 가 아예 안 뜬다 |
| B1-3 | 각 서비스에 `APP_INSTANCE_ID`, `mem_limit`, `stop_grace_period: 30s` |
| B1-4 | 관리 포트를 호스트 쪽에서 둘로 (8081 · 8082). 컨테이너 안은 양쪽 다 8081 이고 Caddy 는 그쪽을 본다 |

`stop_grace_period` 가 없으면 도커 기본 유예 10초가 `timeout-per-shutdown-phase` 20초를 잘라
graceful shutdown 이 실제로는 안 돈다.

### B2 — Caddy

| | |
|---|---|
| B2-1 | 업스트림 2개 + §1 의 헬스체크 · 스티키 |
| B2-2 | `caddy validate` |

### B3 — 관측

| | |
|---|---|
| B3-1 | `prometheus.yml` 타깃 2개, `instance` 라벨 |
| B3-2 | 로그 패턴에 인스턴스 이름 |
| B3-3 | `monitoring.yml` 공통 태그에 `instance` |

### B4 — 배포 워크플로

평시에 둘 다 받는다. **배포는 한쪽씩 바꾸되, 올린 쪽을 거부인 채로 두었다가 전환 때 한 번에
뒤집는다.** 올리자마자 받게 하면 옛 버전과 새 버전이 함께 받는 구간이 생겨 롤링과 같아진다.

```
준비    Redis · DB 접속 확인, Caddyfile validate
        Caddyfile 이 바뀌었으면 여기서만 reload 한다
        blue · green 이 둘 다 없으면 첫 배포 경로로 간다        (아래)

①      green refuse  → Caddy 가 뺀다. blue(옛) 혼자 받는다
        지난 배포 실패로 green 이 꺼져 있으면 건너뛴다
②      green 의 트래픽 상태 파일에 false 를 쓰고 새 이미지로 기동
        올라와도 **계속 거부 상태**다. 트래픽을 받지 않으니 전역 작업도 멈춰 있다
③      green 자체 점검 — 관리 포트 liveness
        실패하면 green 만 내리고 끝낸다. blue(옛) 가 계속 받는다

④      전환 — green accept, 공개 주소로 확인되면 **곧바로** blue refuse
        여기가 두 버전이 함께 받는 유일한 구간이다

⑤      blue 의 트래픽 상태 파일에 false 를 쓰고 새 이미지로 기동
⑥      blue 자체 점검 → accept. 이제 둘 다 새 버전으로 받는다
        실패하면 내린 채로 두고 알린다. green(새) 한 대로 돈다
마무리  .env 에 이미지 고정, 쓰지 않는 이미지 정리
```

**트래픽 상태는 파일로 남긴다. 환경 변수로 넘기지 않는다** ★ 2026-10-09 뒤집음

처음에는 `APP_TRAFFIC_ACCEPT_ON_STARTUP=false` 로 띄우고 전환 때 `/actuator/traffic` 으로 켰다. 켠 상태는 JVM
메모리에만 있고 환경 변수는 컨테이너 설정에 남아, **docker 가 재시작시키면 거부 상태로 떴다.** 2026-10-09 에
blue · green 이 차례로 혼자 재시작해 받는 쪽이 0대가 되어 공개 API 가 503 이 됐다. 이제 인스턴스마다
`./traffic/<색>/accepting` 을 마운트하고, 앱이 기동할 때 읽고 트래픽을 켜고 끌 때마다 고쳐 쓴다.
배포는 올리기 전에 그 파일에 false 를 써 둔다.

**②~③ 동안 용량이 1대로 준다.** 같은 EC2 에 JVM 을 셋 둘 수 없으니 피할 수 없다.
그 대신 그 구간에서 도는 것은 **옛 버전 하나뿐**이라 섞이지 않는다.

③ 의 실패가 가장 흔하고 그때 사용자는 아무것도 못 느낀다. ④ 의 확인이 없으면
`health_interval` 만큼 양쪽이 다 빠진 창이 생긴다. `sleep` 이 아니라 **확인**이어야 한다.

**④ 에서 겹침과 공백 중 하나는 고른다.** blue 에 refuse 를 걸어도 Caddy 가 빼는 데 최대
`health_interval` 이 걸리므로 그동안 요청이 양쪽으로 갈 수 있다. 순서를 뒤집어 blue 를 먼저 빼면
겹치지 않는 대신 green 이 들어올 때까지 받을 곳이 없다. **겹침을 고른다** — §2 의 확장 후 축소가
어차피 그것을 전제하고, 공백은 곧장 5xx 다. 로컬 실측에서 앱 수준의 전환은 84ms 였고,
요청 수준의 겹침은 `health_interval`(2s) 로 묶인다.

| | |
|---|---|
| B4-1 | `--no-deps backend` · `docker exec fungame-backend` 를 두 서비스 기준으로. `PREVIOUS_IMAGE` 도 서비스별로 |
| B4-2 | `/actuator/traffic` 전환 단계. **JSON 본문 POST** 라 컨테이너 안 busybox `wget` 으로 되는지 먼저 확인한다 |
| B4-3 | ④ 의 공개 주소 확인 대기와, 확인 직후의 refuse. 둘 사이가 벌어진 만큼 두 버전이 함께 받는다 |
| B4-4 | ③ 실패 시 green 만 정리하는 경로, ⑥ 실패 시 blue 를 내린 채 알리는 경로 |
| B4-5 | 첫 배포 분기. **green 부터 올린다** — 옛 컨테이너가 관리 포트 8081 을 쥐고 있어 blue(8081)를 먼저 올리면 `port is already allocated` 로 기동 자체가 실패한다. green 은 8082 라 부딪히지 않는다 |
| B4-6 | `reload_caddy` 를 성공 뒤에서 ① 앞으로. reload 는 헬스체크 상태를 초기화해 거부 중인 쪽도 잠깐 살아 있는 것으로 본다 |
| B4-7 | `if: failure()` 복구 스텝에서 설정 롤백과 `up -d --no-deps backend` 를 걷어낸다. 그대로 두면 실패할 때마다 단일 컨테이너 구성으로 되감기고 복원된 Caddyfile 이 없는 `backend:8080` 을 가리킨다 |

**첫 배포는 이 순서다.** 옛 단일 컨테이너 하나만 떠 있는 상태에서 시작한다.

```
① green 을 새 이미지로 (거부 상태)      옛 컨테이너가 계속 받는다. JVM 둘
② green accept → Caddy reload           여기서야 Caddy 가 blue · green 을 알게 된다
③ 공개 주소 확인                        안 되면 옛 컨테이너를 내리지 않고 멈춘다
④ 옛 컨테이너를 내린다                  8081 이 비워진다. JVM 하나
⑤ blue 를 새 이미지로 → accept          JVM 둘
```

**어느 시점에도 JVM 이 셋이 되지 않는다.** 셋을 띄울 이유가 없고, 띄우면 전환 중 메모리만 튄다.
②의 reload 직후 Caddy 가 아직 없는 blue 를 고를 수 있어 502 가 한 번 났었다 —
§1 의 `lb_try_duration` 이 그것을 덮는다.

### B5 — 확인

| | |
|---|---|
| B5-1 | 서버에서 손으로 ①~⑧ 을 한 번 밟는다 |
| B5-2 | 배포 중 공개 주소를 짧은 간격으로 때려 응답 코드를 남긴다 |
| B5-3 | 판이 도는 방을 띄워 두고 배포를 돌려 다음 라운드가 제시간에 오는지 본다 |

로컬에서 2대 + Caddy 로 ①~⑧ 을 그대로 돌려 **회원 4명이 로비 · 방 · 초대 · 입장 · 준비 · 게임 ·
채팅을 하는 동안** 다음을 봤다. 운영에서 다시 볼 것은 같은 항목이다.

| 본 것 | 결과 |
|---|---|
| 공개 주소 가용성 (0.2초 간격) | 202회 **전부 200**. 5xx · 연결 실패 0 |
| 진행 중인 판 | 배포를 건너 다음 라운드가 왔다 (`ROUND_START` 1 → 2) |
| 방 이벤트 | `PLAYER_JOIN` · `PLAYER_READY` · `GAME_START` · `ROUND_START` · `ROUND_HINT` · `ROUND_END` 전원 수신 |
| 채팅 | 4명이 212건씩 받았다. 전송 실패 0 |
| 초대 | `/user/queue/invite` 로 알림이 갔고 수락까지 됐다 |
| 로비 | `/topic/presence` 전원 수신 |
| 끊김 | 1~2회, 복구 570~650ms |
| **첫 배포** | 옛 단일 컨테이너 하나만 있는 상태에서 124회 폴링 **전부 통과**. 아래 참조 |
| 분산 | Caddy 가 두 업스트림에 갈라 보냈고 `lb` 쿠키로 고정됐다 |
| **버전 혼재** | 이미지를 v1 · v2 로 갈라 300ms 간격으로 샘플링해 **둘이 동시에 받는 샘플 0회**. 받는 쪽이 없는 샘플도 0회 |
| 전환 길이 | 앱 수준 84ms. 샘플 간격보다 짧아 샘플링으로는 못 잡는다 — 요청 수준 겹침은 위 §B4 의 `health_interval` 로 묶인다 |

---

## 3. 롤백

| 어디서 | 무엇이 서비스 중 | 무엇을 한다 |
|---|---|---|
| ③ green 점검 실패 | **blue (옛 버전)** | green 만 내린다. 이미지를 다시 받지 않는다 |
| ④ 전환 직후 이상 | green (새 버전) | green 에 refuse, blue 에 accept. 업스트림만 되돌린다 |
| ⑥ blue 점검 실패 | **green (새 버전)** | blue 를 내린 채 두고 알린다. 새 버전 한 대로 돈다 |

⑥ 에서 blue 를 옛 이미지로 되돌리지 않는다. 되돌린 v1 이 받으면 v1·v2 가 동시에 받는 롤링이 되고
끝나는 시점이 없다. 받지 않을 거면 띄울 이유가 없다.

**어느 경우에도 "둘 다 내려간" 상태가 없다.**

---

## 4. 끝났다고 보는 기준

- 배포가 도는 동안 `api.fun-game.club` 이 한 번도 5xx 를 내지 않는다 (B5-2 로 잰다)
- 판이 진행 중인 방이 배포를 건너 살아남고 다음 라운드가 제시간에 온다
- 한쪽을 `docker kill` 해도 남은 쪽이 받는다. 그 인스턴스에 붙어 있던 회원만 방에서 빠진다
- Grafana 에서 두 인스턴스가 `instance` 라벨로 갈려 보인다
- 전환 중 Caddyfile 을 한 번도 고쳐 쓰지 않고, 실패해도 단일 컨테이너 구성으로 되감기지 않는다

SockJS XHR 폴백이 쿠키 스티키로 버티는지는 운영에서 전환을 한 번 해 봐야 안다. 로컬에서는 버텼다.
안 되면 폴백을 끄고 WebSocket 만 쓴다.

**이 구성이 사지 못하는 것** — EC2 나 Caddy 가 죽으면 서비스가 통째로 멈춘다. 2대가 사는 것은
프로세스 단위 장애와 무중단 배포뿐이다. 용량이 모자라면 수직으로 올린다 (상위 §1.11).
