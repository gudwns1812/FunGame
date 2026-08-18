# 커스텀 지표: 로그인과 DAU

작성일: 2026-08-18

## 무엇을 직접 세는가

`support:monitoring` 이 actuator · prometheus 를 붙여 두어 HTTP · JVM · 커넥션풀 지표는 이미 나온다.
그 위에 기본 지표가 볼 수 없는 것 둘만 더했다.

| 지표 | 타입 | 태그 | 어디서 오르나 |
| --- | --- | --- | --- |
| `fungame_logins_total` | Counter | `result=success\|fail` | `AuthService.login` |
| `fungame_member_first_seen_total` | Counter | 없음 | STOMP CONNECT 시 그날 첫 접속 |

둘 다 시도가 없어도 0 으로 등록된다. 첫 이벤트 전까지 시계열이 아예 없으면 알람이
"데이터 없음" 으로 떠 버리기 때문이다.

## 왜 DAU 를 게이지로 두지 않았나

인메모리 게이지는 값의 수명이 프로세스 수명에 묶인다. 오후에 배포하면 그날 나머지 시간을 0 부터
다시 세서 DAU 를 과소 집계하고, 자정 롤오버도 직접 짜야 한다.

카운터로 두면 프로메테우스의 `increase()` 가 재시작에 의한 리셋을 보정하므로 배포해도 그날 수치가
살아남고, rolling 24h DAU 가 덤으로 붙는다. 중복 제거는 `member_daily_active` 의 기본키
`(member_id, active_on)` 가 맡는다 — `insert ignore` 가 1 을 돌려준 접속만 카운터를 올리므로
한 회원은 하루에 정확히 한 번만 반영된다.

## 왜 로그인이 아니라 접속에서 세나

이 서비스는 로그인 한 번으로 세션이 유지된다. 로그인 이벤트를 기준으로 세면 며칠씩 붙어 있는
사용자가 DAU 에서 빠진다. 활동의 실제 신호는 소켓 연결이라 `WebSocketEventListener.handleConnected`
에서 남긴다. 접속마다 DB 를 때리지 않도록 그날 이미 기록한 회원 id 를 메모리에 두고 거른다.
이 메모가 재시작으로 비어도 DB 기본키가 최종 방어선이라 값이 부풀지 않는다.

지표를 남기려다 접속 처리를 깨뜨리지 않는다. DB 가 실패하면 경고만 남기고 메모에서 빼 두어
다음 접속에 다시 시도한다.

## 운영 쿼리

```promql
# DAU (rolling 24h)
sum(increase(fungame_member_first_seen_total[24h]))

# 로그인 실패율
sum(rate(fungame_logins_total{result="fail"}[5m]))
  / sum(rate(fungame_logins_total[5m]))
```

`increase()` 의 리셋 보정은 외삽이라 소수점 오차가 있다. 정산에 쓸 정확한 숫자는 테이블이 진실이다.

```sql
-- 특정 날짜의 DAU
select count(*) from member_daily_active where active_on = ?;

-- 최근 7일 WAU
select count(distinct member_id) from member_daily_active
 where active_on > date_sub(curdate(), interval 7 day);
```

## 태그 규칙

`result` 처럼 값이 손에 꼽히는 것만 태그로 둔다. `memberId` · `roomId` · 닉네임은 값마다 시계열이
하나씩 생겨 카디널리티가 터지므로 넣지 않는다. 개별 추적은 지표가 아니라 로그가 할 일이다.
