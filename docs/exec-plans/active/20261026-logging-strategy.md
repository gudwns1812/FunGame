# 실행 계획: 로그를 요청 · 세션 · 회원 · 방 단위로 묶고, 소음 · 개인정보 · 영어를 걷어낸다

작성 2026-10-26
대상: `backend` `infra`

이 문서가 유일한 계획이다. **남은 일만 적는다.** 끝난 작업은 지우고 코드와 커밋에 맡긴다.

---

## 1. 결정

### 1.1 MDC 키는 다섯 개다

| 키 | 값 | 넣는 곳 |
| --- | --- | --- |
| `requestId` | HTTP 요청마다 새로 만드는 8자리 id | HTTP 필터 |
| `wsSession` | STOMP 세션 id | STOMP 인바운드 인터셉터, 연결 끊김 처리 |
| `memberId` | 인증된 회원 id | HTTP 필터, STOMP 인바운드 인터셉터, 연결 끊김 처리, 회원 대상 예약 작업 |
| `roomId` | 방 id | STOMP 목적지 `/app/room/{id}/...` · `/topic/room/{id}`, 방 대상 예약 작업 |
| `timerTask` | `GameTimerTask.key()` | 예약 작업 |

- 키를 더 늘리지 않는다. `gameId` 처럼 필요해 보이는 값은 이미 메시지 인자로 남고 있다.
- `requestId` 를 응답 헤더로 내보내지 않는다. 지금 쓸 곳이 없다.

### 1.2 MDC 를 채우고 비우는 자리

- **HTTP**: `LogContextFilter` 를 보안 체인의 `AnonymousAuthenticationFilter` 바로 뒤에 둔다. `requireExplicitSave(false)` 라
  세션의 인증은 `SecurityContextHolderFilter` 가 아니라 그 뒤의 `SecurityContextPersistenceFilter` 가 읽어 온다. 인가 필터보다는
  앞이라 401 · 403 응답에도 `requestId` 가 붙는다. 요청이 끝나면 넣은 키만 되돌린다.
- **STOMP**: `ExecutorChannelInterceptor` 의 `beforeHandle` 에서 넣고 `afterMessageHandled` 에서 지운다.
  `preSend` 는 웹소켓 I/O 스레드에서 돌아 실제로 메시지를 처리하는 스레드와 다르다. 기존 `RoomSubscriptionAuthorization` 과
  함께 `configureClientInboundChannel` 에 등록한다.
- **STOMP 연결 끊김**: `SessionDisconnectEvent` 는 Tomcat 스레드에서 처리돼 인터셉터를 지나지 않는다.
  `WebSocketEventListener.handleDisconnect` 가 끊긴 세션의 회원을 찾은 뒤 나머지를 `LogContext` 로 감싼다.
- **비동기**: `AsyncConfig` 의 `applicationTaskExecutor` 에 MDC 를 복사하는 `TaskDecorator` 를 단다. `@Async` 리스너
  (`GameNotifier` · `LobbyNotifier` · 메일 · 신고)가 이 실행기를 쓰므로 원래 요청의 문맥을 그대로 이어받는다.
- **예약 작업**: `GameTimerPoller.run` 에서 `timerTask` 와 대상(`roomId` 또는 `memberId`)을 넣는다.
- `@Scheduled` 정리 작업(`IdleRoomCleaner` 등)과 Redis 브로드캐스트 수신 스레드에는 넣지 않는다. 요청과 이어지지 않고,
  남기는 로그가 이미 방 id 나 목적지를 인자로 들고 있다.
- 비울 때는 `MDC.clear()` 가 아니라 넣은 키만 지운다. 스레드 풀이 다른 문맥을 들고 있을 수 있다.

### 1.3 로그 줄 모양

`logging.pattern.correlation` 을 `"[%X] "` 로 둔다. 스프링 부트 기본 패턴의 문맥 자리(스레드 이름과 로거 사이)에
`[memberId=17, roomId=42, wsSession=ab12cd]` 처럼 찍힌다. 문맥이 없으면 `[]` 다.

- 레벨 자리는 그대로라 Alloy 의 레벨 정규식(`^\S+\s+(LEVEL)\s`)을 고치지 않아도 된다.
- 키 순서는 고정되지 않는다. 검색은 `키=값` 으로 하므로 상관없다.

### 1.4 Loki 에서는 라벨이 아니라 구조화 메타데이터로 뽑는다

`memberId` · `roomId` 를 라벨로 만들면 스트림 수가 회원 수만큼 늘어난다. Alloy 에서 로거 이름 바로 앞의 `[...]` 를
`stage.regex` 로 뽑고, 그 안의 MDC 키 다섯 개를 `stage.structured_metadata` 로 붙인다. Loki 3 · 스키마 v13 이라
따로 켤 것이 없다. 그라파나 로그 대시보드에는 `memberId` · `roomId` 변수를 더해 `| memberId=~"$memberId"` 로 거른다.
변수는 기본값이 All(`.*`)이고 값을 직접 입력할 수 있다. 빈 문자열을 기본값으로 두면 문맥이 없는 줄만 남는다.

### 1.5 개인정보 기준

**남기지 않는다**: 이메일, 로그인 id, 닉네임, 비밀번호, 토큰 · 재설정 링크, 채팅 내용, 사용자가 입력한 값 그대로.
**남긴다**: `memberId` · `roomId` 같은 내부 식별자. 회원 표에 접근하지 않으면 사람을 특정할 수 없다.

| 위치 | 지금 | 바꾼 뒤 |
| --- | --- | --- |
| `SmtpMailSender` | 실패 시 수신자 이메일 · 제목 | 제목만 |
| `PasswordResetMailListener` | 실패 시 수신자 이메일 | 수신자를 빼고 `requestId` 로 요청과 잇는다. 이벤트에 이메일밖에 없다 |
| `ApiControllerAdvice` `BindException` | `e.getMessage()`, 거부된 값(비밀번호 · 이메일)이 들어 있다 | 필드 이름과 검증 코드만 |
| `LoggingPasswordResetMailSender` (`!prod`) | 이메일 · 재설정 링크 | 링크만. 로컬에서 링크를 받아 쓰는 것이 이 클래스의 목적이다 |

- 처리하지 못한 `Exception` 의 메시지 안에 들어간 값(예: DB 고유 제약 위반 메시지의 중복 이메일)은 이번에 막지 못한다.
  예외 메시지를 일반적으로 가리면 원인을 볼 수 없게 된다. 실제로 보이면 그 경로를 `CoreException` 으로 바꿔 막는다.
  `SmtpMailSender` 가 함께 남기는 `MailSendException` 도 같다. 주소를 거부당해 실패하면 SMTP 응답에 받는 주소가 들어 있다.

### 1.6 INFO 정리 기준

INFO 는 **운영 중 상태가 바뀐 사건**만 남긴다. 정상 흐름을 한 단계씩 알리는 줄은 지운다. 흔히 일어나는 경합은 DEBUG 로 내린다.

| 위치 | 처리 | 이유 |
| --- | --- | --- |
| `GameNotifier` `Broadcasting ...` 11줄 | 삭제 | 라운드 · 힌트 · 준비마다 찍혀 INFO 의 절반이다. 보낸 내용은 클라이언트가 받는다 |
| `QuizGameService` `"게임 종료"` · `"라운드 종료"` | 삭제 | 방 id 조차 없다 |
| `ChatController` 방 없음 2줄 | `ErrorType` 이 정한 레벨 | 방이 사라진 경합(`GAME_ROOM_NOT_FOUND`)은 DEBUG 다. 같은 catch 가 행맨의 `INVALID_INPUT_VALUE` · `GAME_NOT_FOUND`(WARN)도 잡아 일괄 DEBUG 면 묻힌다 |
| `RoomLeaveGrace` "이미 없다" | DEBUG | 같은 경합이다 |
| 나머지 INFO (`TrafficGate`, 퇴장 · 재입장 · 이탈, 유휴 방 정리, 전적 중복, 곡 저장) | 유지 | 드물고, 사건 하나가 한 줄이다 |

### 1.7 메시지는 한국어, `~했다` 체다

- 영어 메시지(`ChatController` · `StompDestination` · `ApiControllerAdvice` 의 `BindException` · `Exception`)를 한국어로 바꾼다.
- 한국어 문장 속 `room {}` · `member {}` 를 `방 {}` · `회원 {}` 으로 바꾼다.
- `~했습니다` 체(`SmtpMailSender` · `DiscordWebhookSender` · `PasswordResetMailListener`)를 다수인 `~했다` 체로 맞춘다.
- 오류 코드(`{} : {}` 의 앞부분)와 클래스 · 예외 이름은 그대로 둔다.

### 1.8 다시 생기지 않게 커밋 검사를 하나 더한다

기존 `comment-sweep` · `no-inline-fqn` 과 같은 방식으로 `log-message` 검사를 `commit-gate.sh` 에 붙인다.
이번 변경이 **새로 추가한** `log.*(` 호출만 본다.

- 메시지 문자열에 한글이 하나도 없으면 실패한다. `"{} : {}"` 처럼 자리표시자와 기호만 있는 메시지는 통과다.
- 인자 식에 `email` · `password` · `token` · `nickname` · `loginId` 가 들어 있으면 실패한다.
- 이름으로 거르는 것이라 완전하지 않다. 예외로 넘겨야 하면 다른 검사처럼 `LOG_MESSAGE=skip` 을 붙이고 이유를 말한다.

### 1.9 하지 않는 것

- 분산 추적(Micrometer Tracing · OpenTelemetry). 지금은 서비스가 하나라 `requestId` 로 충분하다.
- JSON 구조화 로그로 바꾸기. 패턴과 구조화 메타데이터로 필요한 검색이 된다.
- Loki 보존 기간 · Docker 로그 로테이션 · Loki 3100 공개 문제. 따로 다룬다.
- 프론트엔드 `console` 로그.

---

## 2. 남은 일

구현과 로컬 검증은 끝났다(gudwns1812/FunGame#122). 남은 것은 배포와 운영 확인이다.

1. **배포.** 백엔드와 Alloy 설정은 백엔드 배포 워크플로가 한 번에 올린다. 대시보드는 `deploy-infra` 로 나가며,
   백엔드보다 먼저 나가도 변수 기본값(All)에서는 지금과 같다.
2. **운영 확인.** 로그 대시보드에서 `회원 id` · `방 id` 로 걸러지는지, 로그 상세에 구조화 메타데이터가 붙는지,
   INFO 양이 줄었는지 본다.
3. 확인되면 이 문서를 지운다.
