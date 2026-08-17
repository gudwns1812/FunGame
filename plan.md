# 작업 계획: 타이머 tick 브로드캐스트를 걷어내고 남은 시간은 클라이언트가 센다

브랜치: `refactor/client-side-round-timer` (#67)

지금 라운드 남은 시간은 서버가 1초마다 `TIMER_TICK` 으로 브로드캐스트하고, 클라이언트는 받은 숫자를
그대로 화면에 적는다. 표시 단위와 전송 단위가 같아서 프레임 하나가 늦으면 숫자가 멈췄다가 두 칸 뛴다.

**서버는 라운드가 언제 끝나는지 한 번 알려주고, 초 단위 진행은 클라이언트가 센다.**
권위는 서버에 남는다 — 클라 카운터가 0에 닿는 것은 화면상의 일이고, 라운드가 실제로 끝나는 것은
서버의 `ROUND_END` 를 받았을 때다.

## 먼저 확인한 것 — 라운드 길이는 30초다

`scheduleAtFixedRate(task, Duration)` 는 첫 실행을 곧바로 돈다. 그래서 지금 tick 은
`t=0s → remain=30`(라운드 시작 순간), `t=20s → remain=10`(힌트), `t=30s → remain=0`(타임아웃) 이다.
즉 라운드 길이는 정확히 30초이고, 힌트는 시작 20초 뒤에 열린다. 이 두 시점을 절대 오프셋으로
옮기면서 라운드 길이가 1초도 달라지지 않아야 한다.

## 1단계 — 방당 예약을 여럿 담을 수 있게 한다 (`GameTimer`)

지금 `roomTasks` 는 `Map<Long, ScheduledFuture<?>>` 라 방당 태스크가 하나뿐이고,
`startAfter` 가 시작할 때마다 `stop(roomId)` 으로 이전 것을 지운다. 힌트와 타임아웃을 동시에
예약하려면 이 구조로는 안 된다.

- `Map<Long, Collection<ScheduledFuture<?>>>` 로 바꾸고 `startAfter` 는 **더하기만** 한다.
- `stop(roomId)` 은 그 방의 예약 **전부**를 취소한다. 정답이 나와 라운드가 조기 종료되면
  아직 안 터진 힌트 예약도 함께 사라진다.
- 끝난 예약이 방마다 쌓이지 않게 더할 때 `isDone()` 인 것을 걷어낸다.
- 초(`int`) 대신 `Duration` 을 받는다. 호출부에서 단위를 헷갈릴 자리를 없앤다.
- `startCountDown` 은 쓰는 곳이 없어지므로 지운다.

`gameTimer.stop` 은 `GameRoomManager.deleteRoom` · `endGame` 에서도 부르고 있어 방이 사라지거나
게임이 끝나면 예약이 남지 않는다.

## 2단계 — 라운드에 시계를 달고 남은 시간을 재게 한다

`GameSession` 은 라운드 시작 시각을 갖고 있지 않다. `RoundClock` 을 새로 두고 `GameSession` 이 갖는다.

- `RoundClock` — 라운드 길이(30초)와 힌트가 열리는 시점(남은 10초)을 갖는다.
  `start()` 가 시작 시각을 찍고, `stop()` 이 지우고, `remainingMillis()` 가 0 아래로는 안 내려간다.
- `GameSession.startRound()` 이 시계를 켠다. `startProcessing()` 이 true 를 돌려준 호출
  — 그 라운드를 닫는 단 한 번의 호출 — 이 시계를 끈다. 그래서 라운드 사이 3초 동안은 남은 시간이 0이다.

## 3단계 — tick 을 걷어내고 시점 예약 두 개로 바꾼다 (`QuizGameService`)

```java
timer.startAfter(roomId, gameSession.untilHintOpens(), () -> publishHint(roomId, gameSession));
timer.startAfter(roomId, gameSession.roundLength(), () -> endRound(roomId, NO_WINNER));
```

- `TimerTickEvent` 와 `GameNotifyService.handleTimerTicker` 를 지운다.
- `RoundStartEvent` 에 `remainingMillis` 를 싣고 `ROUND_START` payload 로 내려준다.
  **절대 시각(epoch millis)이 아니라 남은 밀리초**를 보낸다. 클라 시계가 서버와 어긋나도
  프레임을 받은 자기 시각이 기준점이 되므로 오차 상한이 네트워크 편도 지연으로 묶인다.
- 흩어져 있던 5초 · 3초 · 30초를 `private static final Duration` 상수로 올린다.
- 행맨은 라운드 시계가 없으므로 `NO_ROUND_CLOCK`(0) 을 싣는다.

## 4단계 — 라운드 도중에 들어온 사람

tick 이 없어지면 `ROUND_START` 를 놓친 사람은 남은 시간을 알 방법이 사라진다.
`GET /game/rooms/{roomId}/play/state` 스냅샷에 남은 시간을 포함한다.

- `GameStateDto` · `GameStateResponse` 에 `remainingMillis` 를 더한다.
- `QuizGameService.getPlayState` 는 `gameSession.remainingRoundMillis()` 를, 행맨은 0을 돌려준다.
- 라운드 길이(진행바의 분모)는 스냅샷에 싣지 않는다. 화면 기본값이 30초이고 서버 정책도 30초라
  같은 값을 두 곳에서 말하게 만들 이유가 없다.

## 5단계 — 클라이언트가 센다 (`useGameLogic`)

- `TIMER_TICK` 케이스와 `types/game.ts` 의 유니온 멤버를 지운다.
- `ROUND_START` · 스냅샷 복원에서 **마감 시각**(`Date.now() + remainingMillis`)을 잡는다.
- 200ms 간격으로 `ceil((마감 - 지금) / 1000)` 을 다시 구한다. 누적 카운트가 아니라 매번
  기준점에서 다시 구하는 방식이라 백그라운드 탭에서 `setInterval` 이 throttle 돼도 어긋나지 않고,
  탭이 다시 보이는 순간 `visibilitychange` 로 곧바로 맞춘다.
- 값이 같으면 React 가 리렌더를 건너뛰므로 200ms 폴링 비용은 없다.
- 0 아래로는 내려가지 않고, 0에서 `ROUND_END` 를 기다리는 구간에도 입력을 막지 않는다.
  중복 종료는 서버 `startProcessing()` 이 이미 막고 있으므로 판정은 서버에 맡긴다.

## 테스트

- `GameTimerTest` — 한 방에 예약 둘이 함께 살아 있다 / `stop` 이 전부 취소한다 /
  끝난 예약이 쌓이지 않는다
- `RoundClockTest` — 켜지면 라운드 길이만큼 남는다 / 흐른 만큼 줄어든다 / 0 아래로 안 내려간다 /
  꺼지면 0이다
- `QuizGameServiceTest` — 라운드가 시작되면 힌트와 타임아웃을 **각각** 예약한다 /
  두 예약 시점이 20초 · 30초다 / `ROUND_START` 에 남은 시간이 실린다 /
  스냅샷에 남은 시간이 실린다 / 라운드가 닫히면 남은 시간이 0이다
- `GameServiceIntegrationTest` — `Duration` 시그니처에 맞추고, 라운드 자체의 힌트·타임아웃
  예약은 테스트에서 즉시 실행하지 않는다 (즉시 실행하면 라운드가 시작과 동시에 타임아웃된다)
- `useGameLogicRoundTimer.test.ts` — `ROUND_START` 로 카운트가 시작된다 / 서버가 아무것도 더 보내지
  않아도 줄어든다 / 0 아래로 안 내려간다 / `ROUND_END` 가 오면 그 자리에서 멈춘다 /
  스냅샷 복원으로 라운드 중간부터 센다 / 라운드 사이에 들어오면 세지 않는다

## 문서

- `api/websocket.md` — `TIMER_TICK` 행 삭제, `ROUND_START` 에 `remainingMillis`
- `api/game.md` — `GameStateDto` 에 `remainingMillis`
- `frontend/docs/FunGame_API.md` — 이벤트 명세 · 흐름도에서 `TIMER_TICK` 제거
- `frontend/guideLine.md` — 이벤트 표와 유튜브 대조 항목 갱신

## 구현하면서 계획과 달라진 것

- **`stopCountDown` 이 멈추기 전에 남은 시간을 한 번 더 적는다.** 브라우저로 돌려 보니 라운드가
  시간을 다 써서 끝나는 순간 화면이 `1초` 에서 얼어붙었다. `ROUND_END` 가 마감 시각과 거의 동시에
  도착해 카운터가 0을 적을 틈이 없었던 것이다. tick 시절에는 마지막 프레임(`remainingSeconds: 0`)이
  0을 적어줬으니 이대로면 화면이 후퇴한다. 멈추는 순간 마감 시각 기준으로 한 번 더 계산해서
  타임아웃은 0, 정답으로 인한 조기 종료는 그때 남아 있던 값으로 멈추게 했다.
- **흩어져 있던 5초·3초도 상수로 올리고 `endRound(roomId, null)` 에 `NO_WINNER` 이름을 붙였다.**
  라운드 길이만 상수로 만들면 같은 파일에서 30은 이름이 있고 5·3은 숫자로 남는다.
- **`GameServiceIntegrationTest` 는 5초를 넘는 예약을 흘려보낸다.** 예약을 즉시 실행하는 스텁이라
  라운드의 힌트(20초)·타임아웃(30초)까지 즉시 돌면 라운드가 시작과 동시에 타임아웃된다.
  게임 흐름 전환(5초·3초)만 즉시 실행한다.
- **`FunGame_API.md` 의 `TIMER_TICK` 절을 `ROUND_START` 절로 갈았다.** 그 문서에는 `ROUND_START`
  절이 아예 없어서 지우기만 하면 남은 시간을 어디서 얻는지 적힌 자리가 사라진다.

## 검증

- 백엔드: `./gradlew clean build` 통과
- 프론트: `npx vitest run` (33 파일 203 테스트), `npx tsc --noEmit`, `npx eslint`(기존과 동일), `npx vite build`
- **로컬 서버를 띄우고 raw WebSocket 으로 붙어 프로토콜 검증 20/20** — `ROUND_START` 의
  남은 시간 30000ms, 힌트 +20.0s, 라운드 길이 +30.0s(31초로 늘지 않음), `TIMER_TICK` 0프레임,
  정답·스킵 조기 종료, 조기 종료된 라운드에 힌트 예약이 뒤늦게 터지지 않음, 라운드 도중 스냅샷의
  남은 시간이 흐른 만큼 줄어듦, 라운드 사이 스냅샷은 0, 남은 시간이 한 번도 음수가 되지 않음
- **크로미움 두 창으로 실제 플레이해 화면 검증 12/12** — 30초부터 1초에 한 칸씩(간격 0.92~1.07s),
  서버 프레임 없이 12칸 진행, 탭을 5초 숨겼다 돌아와도 정확히 5초 줄어 있음, 0까지 내려가고
  음수 없음, 0에서도 입력창 살아 있음, 브라우저가 받은 프레임에 `TIMER_TICK` 없음,
  새로고침 뒤 스냅샷의 남은 시간(24초)으로 이어서 계속 셈
