# 작업 계획: 로그인 지표와 DAU 를 카운터로 노출한다

브랜치: `feat/login-and-dau-metrics`

`support:monitoring` 이 이미 actuator · prometheus 를 붙여 두어 HTTP · JVM · 커넥션풀은 공짜로 나온다.
여기에 **기본 지표가 볼 수 없는 것 두 가지**를 더한다.

1. **로그인 성공/실패** — 인증 장애와 크리덴셜 스터핑은 `http.server.requests` 만으로는 안 보인다.
   `/api/auth/login` 이 실패해도 200 이 아닌 응답 코드 하나로 뭉뚱그려지고, 성공/실패 비율이 남지 않는다.
2. **DAU** — 지금 회원의 활동 시각을 어디에도 적지 않아 "오늘 몇 명이 왔나" 를 답할 방법이 없다.

## 왜 게이지가 아니라 카운터인가

인메모리 게이지로 세면 값의 수명이 프로세스 수명에 묶인다. 오후에 배포하면 그날 나머지 시간을
0부터 다시 세서 DAU 를 과소 집계하고, 자정 롤오버 리셋도 직접 짜야 한다.

단조증가 카운터로 두면 `sum(increase(fungame_member_first_seen_total[24h]))` 로 DAU 가 나온다.
`increase()` 가 **재시작에 의한 카운터 리셋을 보정**하므로 배포해도 그날 수치가 살아남고,
rolling 24h DAU 가 덤으로 붙는다. 중복 제거는 DB unique 키가 하므로 한 회원은 하루에 정확히 한 번만
카운터를 올린다.

`increase()` 의 리셋 보정은 외삽이라 소수점 오차가 있다. 정확한 숫자가 필요하면
`member_daily_active` 를 직접 세면 되고, 그 테이블이 언제나 진실이다.

## 1단계 — 모니터링 모듈이 미터 레지스트리를 밖으로 연다

`support:monitoring` 은 actuator 를 `implementation` 으로 갖고 있어 `MeterRegistry` 타입이
`core:core-api` 컴파일 경로에 없다. `api 'io.micrometer:micrometer-core'` 를 더해 계측에 필요한
타입만 노출한다. actuator 전체를 `api` 로 올리면 엔드포인트 · 시큐리티 설정까지 새어 나간다.

## 2단계 — 하루에 한 행씩 쌓는 테이블 (`storage:db-core`)

```sql
create table member_daily_active (
    member_id bigint not null,
    active_on date not null,
    primary key (member_id, active_on)
) engine = InnoDB;
```

- `(member_id, active_on)` 복합 기본키가 중복 제거를 맡는다. 별도 인덱스를 두지 않는다 —
  DAU 집계(`where active_on = ?`)는 이 테이블이 DAU×일수 규모라 풀스캔으로도 충분하고,
  리텐션 조회는 기본키의 선두 컬럼(`member_id`)을 탄다.
- `member` 로의 FK 는 건다. 탈퇴한 회원의 흔적을 남길 이유가 없고 정리도 따라온다.

`MemberActivityDao` 는 `insert ignore` 로 upsert 하고 **영향받은 행 수**를 돌려준다.
`on duplicate key update` 는 갱신 시 2 를 돌려줘 `== 1` 비교가 헷갈리므로 쓰지 않는다.
`SongUpsertDao` 와 같은 `JdbcTemplate` 방식이다.

## 3단계 — 그날 처음인 사람만 센다 (`domain/member/DailyActiveMembers`)

- `record(memberId)` 가 `insert ignore` 로 1을 받았을 때만 `fungame.member.first.seen` 을 올린다.
- 접속마다 DB 를 때리지 않도록 **오늘 이미 찍은 회원 id 를 메모리 Set 으로 거른다.**
  날짜가 바뀌면 Set 을 비운다. 이 Set 이 재시작으로 틀려도 DB unique 가 최종 방어선이라 안전하다.
- 날짜 기준은 주입된 `Clock` 이다. 테스트에서 `MutableClock` 으로 자정을 넘긴다.

호출 지점은 **로그인이 아니라 STOMP CONNECT** (`WebSocketEventListener.handleConnected`) 다.
이 서비스는 로그인 한 번으로 세션이 유지되므로 로그인 이벤트 기준으로 세면 며칠씩 붙어 있는
사용자를 놓친다. 활동의 실제 신호는 소켓 연결이다.

## 4단계 — 로그인 성공/실패 (`domain/member/LoginMetrics`)

- `fungame.logins{result=success|fail}`.
- 두 카운터를 **생성 시점에 미리 등록**한다. 첫 로그인 전까지 시계열이 아예 없으면
  `rate()` 알람이 데이터 없음으로 뜬다.
- `AuthService.login` 이 `AuthenticationException` 을 잡아 `fail` 을 세고 그대로 다시 던진다.
  예외는 지금처럼 `ApiControllerAdvice` 가 처리한다 — 응답은 달라지지 않는다.

## 태그 규칙

`result` 처럼 값이 손에 꼽히는 것만 태그로 둔다. `memberId` · `roomId` · 닉네임은 넣지 않는다.
프로메테우스 시계열이 값마다 하나씩 생겨 카디널리티가 터진다.

## 테스트

- `MemberActivityDaoTest` (통합) — 처음 넣으면 1 / 같은 날 다시 넣으면 0 / 날짜가 다르면 1 /
  회원이 다르면 1
- `DailyActiveMembersTest` — 처음 온 회원에 카운터가 오른다 / 같은 날 두 번째 접속은 DB 를 보지 않는다 /
  날짜가 바뀌면 다시 센다 / DB 가 0 을 주면(다른 경로로 이미 기록) 카운터가 오르지 않는다
- `LoginMetricsTest` — 등록만으로 두 카운터가 0 으로 존재한다 / 성공·실패가 각 태그로 쌓인다
- `AuthServiceLoginMetricsTest` — 성공하면 success 가 오른다 / 인증 실패면 fail 이 오르고 예외는 그대로 나간다
- `ManagementEndpointTest` — `/actuator/prometheus` 응답에 두 지표 이름이 실린다

## 문서

- `backend/ARCHITECTURE.md` 에는 손댈 것이 없다(모듈 구성 그대로).
- 운영 쿼리(DAU · WAU · 로그인 실패율)는 `docs/design/20260818-custom-metrics.md` 에 남긴다.

## 구현하면서 계획과 달라진 것

- **DB 실패를 삼키고 메모에서 되돌린다.** `record` 가 STOMP CONNECT 처리 한가운데서 도므로
  DAO 예외가 그대로 올라가면 지표를 남기려다 접속 자체를 깨뜨린다. 경고만 남기고 메모에서 빼
  다음 접속에 다시 시도하게 했다. 이 경로를 테스트로 고정했다.
- **`WebSocketEventListenerTest` 생성자가 인자 하나 늘었다.** 접속 시 활동일을 남기는지,
  회원 정보 없는 세션에서는 남기지 않는지를 그 테스트에 함께 넣었다.
- **`MemberActivityDaoTest` 는 테스트마다 날짜를 달리 쓴다.** 통합 테스트가 행을 되돌리지
  않아 같은 날짜를 공유하면 실행 순서에 따라 결과가 흔들린다.

## 검증

- **단위 테스트 통과** — `DailyActiveMembersTest`(6), `AuthServiceLoginMetricsTest`(4),
  `WebSocketEventListenerTest`(5) 전부 통과. 전체 스위트 기준 통과 수가 277 → 287 로 늘었다.
- **통합 테스트는 이 PC 에서 못 돌렸다.** Docker Engine 29 와 testcontainers 1.20.5(부트 3.4.3 이
  물고 오는 버전) 가 맞지 않아 `/info` 가 400 을 준다. 손대지 않은 기존 `MemberPersistenceTest` 도
  똑같이 실패하므로 이 변경과 무관하다. HEAD 에서 같은 스위트를 돌린 결과가 358개 중 81 실패,
  이 브랜치가 373개 중 86 실패로, 늘어난 실패 5개는 전부 컨테이너가 필요한
  `MemberActivityDaoTest`(4) 와 `ManagementEndpointTest` 에 새로 넣은 1개다.
  **`MemberActivityDaoTest` · `ManagementEndpointTest` 는 CI 에서 확인이 필요하다.**
