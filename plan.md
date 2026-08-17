# 작업 계획: SSE 를 걷어내고 실시간 통신을 WebSocket 하나로 통합한다 (#56)

브랜치: `refactor/websocket-only-realtime`

이슈가 제시한 4단계를 각각 하나의 커밋으로 만든다. 단계마다 독립적으로 돌아가는 상태를 유지한다.

## 1단계 — 방 소속의 제어점을 API 로 옮긴다

**백엔드**

- `GameRoom` 에 단조증가 `version` 을 둔다. 참가·이탈·강퇴·준비·설정변경·시작·종료에서 올린다.
- `RoomStateInfo(roomId, version, status, settings, players, host)` 를 방 상태의 단일 스냅샷 타입으로 만들고
  `PlayersInfo` · `RoomSettingsInfo` 를 없앤다.
- `JoinResult` · `LeaveResult` · `ReadyResult` 와 새 `KickResult` 가 락 안에서 뜬 스냅샷을 함께 들고 나온다.
- 방 이벤트(`PlayerJoinEvent` 등)가 스냅샷을 싣고, `GameNotifyService` 는 `room` 필드로 전체 상태를 내보낸다.
- `RoomSubscriptionAuthorization` 채널 인터셉터가 `SUBSCRIBE /topic/room/{id}` 를 소속 여부로만 인가한다.
  소속이 아니면 구독을 등록하지 않는다(연결은 끊지 않는다).
- `RoomPresence` · `RoomMember` · `RoomMemberKey` · `RoomListReader` · `RoomConnectionRegistry` 를 없애고
  `StompSessions`(세션↔회원) 와 `RoomLeaveGrace`(회원 단위 유예 청소) 로 나눈다.
  유예가 만료되면 그때 회원의 현재 위치를 다시 읽어 거기서 내보낸다.
- 방 목록의 인원수는 접속 세션 수가 아니라 방에 들어 있는 참가자 수다.

**프론트엔드**

- `join API → subscribe → 스냅샷 조회` 순서로 정리한다. 스냅샷 조회를 `onConnect` 안으로 옮긴다.
- 방 이벤트에 실린 전체 상태를 그대로 적용하고, 자기 버전보다 낮은 것은 버린다. 이벤트마다 `/users` 를 다시
  긁는 동작을 없앤다.

## 2단계 — WS 연결을 앱 레벨로 올린다

- `StompProvider` 를 만들어 로그인 시점에 `Client` 를 하나만 만든다.
- 구독은 "연결 하나의 수명" 동안만 산다. 소비자는 `onConnection(setUp)` 으로 연결마다 할 일을 등록한다.
  방은 `join → subscribe → 스냅샷` 을, 로비는 `subscribe → 목록 조회` 를 그 안에서 한다.
- `useGameLogic` 은 `Client` 를 만들지 않고 구독·발행만 한다.

## 3단계 — 로비/초대/접속자를 STOMP destination 으로 옮긴다

- `/topic/lobby`, `/user/queue/presence`, `/user/queue/invite` 를 추가한다.
- `LobbyNotifyService` · `InviteNotifyService` 가 `SimpMessagingTemplate` 을 쓴다.
  접속자 목록은 뷰어별 payload 라 `convertAndSendToUser` 로 보낸다(Principal 이름은 회원 번호다).
- 프론트의 `useOnlineMembers` · `useRoomInvites` · 방 목록 구독을 STOMP 구독으로 교체한다.

## 4단계 — SSE 를 걷어낸다

- `SseController` · `SseService` · `SseConnection` · `MemberPayload` · `SseContext` 와 관련 테스트를 지운다.
- `MemberConnectionTracker` 를 STOMP 세션 CONNECT/DISCONNECT 에 붙인다.
- 문서(`index.adoc`)의 SSE 절을 지운다.

## 검증

- 백엔드: `./gradlew :backend:core:core-api:test`
- 프론트엔드: `npx vitest run`, `npx tsc --noEmit`, `npx vite build`
- 로컬 통합: 앱을 띄워 두 브라우저 세션으로 방 이동·새로고침·탭 닫기·강제 종료·유예 만료를 확인한다.
