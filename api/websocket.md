# WebSocket 및 실시간 이벤트 (websocket.md)

이 문서는 WebSocket(STOMP)을 통한 실시간 통신 명세와 서버에서 발생하는 이벤트 메시지 형식을 정의합니다. 실제 백엔드 `ChatController` 및 `GameNotifier` 로직과 동기화되었습니다.

## 1. 연결 정보 (Connection)

- **Endpoint**: `/ws-quiz`
- **Protocol**: SockJS 지원
- **Message Broker**:
    - **구독 경로(Subscribe)**: `/topic/room/{roomId}` · `/topic/presence`
    - **발행 경로(Publish)**: `/app/room/{roomId}/chat`

## 2. 클라이언트 송신 (Client to Server)

### 채팅 및 정답 입력
유저가 입력한 텍스트를 서버로 전송합니다. 일반 채팅 메시지이거나 퀴즈의 정답 후보일 수 있습니다.

- **Destination**: `/app/room/{roomId}/chat`
- **Header**: `playerName: 닉네임` (필수)
- **Content-Type**: `application/json`
- **Payload (ChatRequest)**: `{ "message": "유저가 입력한 메시지 내용" }`

`text/plain` 으로 문자열만 보내면 `ChatController` 가 `ChatRequest` 로 변환하지 못해
`MessageConversionException` 으로 조용히 버려집니다. 보낸 쪽에는 아무 응답도 가지 않습니다.

## 접속자 목록 (`/topic/presence`)

접속 상태나 방 소속이 바뀌면 서버가 접속자 **전체 목록 한 건**을 모든 구독자에게 보낸다. 변경이 몰리면 0.5초 주기로 한 번만 보낸다.
받는 사람마다 같은 목록이 오므로 **자기 자신은 클라이언트가 뺀다.** 처음 목록은 `GET /api/members/online` 으로 가져온다(이쪽은 자기 자신이 빠져 있다).

```json
{
  "result": "SUCCESS",
  "data": [
    { "memberId": 1, "nickname": "방장", "status": "WAITING", "currentRoomId": 9 },
    { "memberId": 2, "nickname": "참가자", "status": "LOBBY", "currentRoomId": null }
  ]
}
```

`status` 는 `LOBBY` · `WAITING` · `PLAYING` 이고, `currentRoomId` 는 로비에 있으면 `null` 이다.

## 3. 서버 브로드캐스트 이벤트 (Server to Client)

서버는 특정 방에서 발생하는 모든 이벤트를 `/topic/room/{roomId}`를 구독 중인 모든 클라이언트에게 전송합니다.

### 공통 메시지 구조
```json
{
  "type": "EVENT_TYPE",
  "...": "Data Fields"
}
```

### EVENT_TYPE 상세 명세

| Type | 설명 | 데이터 필드 예시 |
| :--- | :--- | :--- |
| **`PLAYER_JOIN`** | 플레이어 입장 | `{ "player": "닉네임" }` |
| **`PLAYER_LEAVE`** | 플레이어 퇴장 | `{ "player": "닉네임" }` |
| **`PLAYER_READY`** | 플레이어 준비 상태 변경 | `{ "player": "닉네임" }` |
| **`HOST_CHANGE`** | 방장 변경 | `{ "newHost": "새방장닉네임" }` |
| **`CHAT`** | 채팅 메시지 | `{ "playerName": "닉네임", "message": "내용" }` |
| **`GAME_START`** | 게임 시작 알림 | `{ "gameType": "SONG", "category": "KPOP", "songCount": 10, "message": "설명문구" }` |
| **`ROUND_START`** | 라운드 시작 | `{ "round": 1, "totalRound": 10, "content": "문제내용", "remainingMillis": 30000 }` |
| **`ROUND_SKIP`** | 스킵 투표 현황 | `{ "skipCount": 2, "totalCount": 5 }` |
| **`ROUND_HINT`** | 퀴즈 힌트 제공 | `{ "hint": "가수 - 초성힌트" }` |
| **`ROUND_END`** | 라운드 종료/정답 | `{ "answer": "정답문구", "winner": "승자닉네임" }` |
| **`GAME_RESULT`** | 최종 게임 결과 | `{ "rankings": "유저1:150\n유저2:100\n", "message": "종료 알림" }` |
| **`GAME_END`** | 세션 종료 | `{ "type": "GAME_END" }` |

---
- **주의**: `GAME_RESULT`의 `rankings` 필드는 줄바꿈(`\n`)으로 구분된 문자열 형식이므로 프론트엔드에서 파싱이 필요합니다.
- **주의**: 모든 닉네임 관련 필드명(`player`, `playerName`, `newHost`, `winner`)이 이벤트 타입마다 다를 수 있으니 위 표를 정확히 참고하세요.
