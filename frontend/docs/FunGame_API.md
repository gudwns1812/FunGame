# 🎵 Song Quiz - 프론트엔드 API 명세서

> **게임 개요**: 노래 맞히기 퀴즈 게임. 플레이어들이 방에 입장하면 방장이 게임을 시작하고, 음악이 재생되는 동안 채팅창에 정답을 입력해 점수를 획득합니다.

---

## 📐 공통 규칙

### Base URL
```
http://{서버주소}
```

### 모든 HTTP 응답 구조
모든 REST API는 아래 형식으로 응답합니다.

```json
{
  "result": "SUCCESS" | "FAIL",
  "data": "Object" | "null",
  "error": {
    "code": "에러코드",
    "message": "에러 메시지"
  } | null
}
```

- `result`가 `"SUCCESS"`이면 `data`에 실제 데이터가 담깁니다.
- `result`가 `"FAIL"`이면 `error`에 에러 정보가 담깁니다.

---

## 🌐 REST API (HTTP)

### 1. 방 목록 조회

> 현재 존재하는 모든 게임방 목록을 가져옵니다. 로비 화면에서 입장 가능한 방을 보여줄 때 사용합니다.

- **Method**: `GET`
- **Path**: `/game/rooms`
- **Request Header**: 없음
- **Request Body**: 없음

**Response `data`**:
```json
[
  {
    "roomId": "abc123",
    "title": "K-POP 퀴즈방",
    "hostName": "방장닉네임",
    "maxPlayers": 8,
    "currentPlayers": 3
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `roomId` | `String` | 방 고유 ID (다른 API에서 `{roomId}`로 사용) |
| `title` | `String` | 방 제목 |
| `hostName` | `String` | 방장 닉네임 |
| `maxPlayers` | `int` | 최대 입장 가능 인원 |
| `currentPlayers` | `int` | 현재 입장한 인원 수 |

---

### 2. 방 생성

> 새로운 게임방을 만듭니다. 방 만들기 버튼 클릭 시 호출합니다. 성공하면 생성된 방의 ID를 반환하며, 이를 이용해 방에 입장합니다.

- **Method**: `POST`
- **Path**: `/game/rooms`
- **Request Header**: `Content-Type: application/json`

**Request Body**:
```json
{
  "title": "방 제목",
  "maxPlayers": 8,
  "hostName": "방장닉네임",
  "category": "KPOP"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | `String` | ✅ | 방 제목 |
| `maxPlayers` | `int` | ✅ | 최대 인원 (예: 2~8) |
| `hostName` | `String` | ✅ | 방장 닉네임 |
| `category` | `String` | ✅ | 음악 카테고리 (아래 참고) |

**`category` 가능한 값**:
- `KPOP` - K-POP
- `POP` - 팝
- `BALLADE` - 발라드
- `RAP` - 랩/힙합
- `OST` - OST

**Response `data`**: `String` - 생성된 방의 ID
```json
{
  "result": "SUCCESS",
  "data": "abc123",
  "error": null
}
```

---

### 3. 방 입장

> 기존 게임방에 참가합니다. 방 목록에서 방을 선택하면 호출합니다. 성공하면 웹소켓 연결 후 해당 방을 구독합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/join`
- **Request Header**: `nickname: 플레이어닉네임` (**필수**)
- **Request Body**: 없음

**Response `data`**: `null` (성공 여부만 확인)
```json
{
  "result": "SUCCESS",
  "data": null,
  "error": null
}
```

> ⚠️ 방 입장 성공 후 웹소켓(`/ws-quiz`)에 연결하고 `/subscribe/room/{roomId}`를 구독해야 실시간 이벤트를 받을 수 있습니다.

---

### 4. 방 퇴장

> 게임방에서 나갑니다. 뒤로가기 또는 나가기 버튼 클릭 시 호출합니다. 방장이 나가면 다른 플레이어에게 방장이 자동으로 넘어갑니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/leave`
- **Request Header**: `nickname: 플레이어닉네임` (**필수**)
- **Request Body**: 없음

**Response `data`**: `null`

> ℹ️ 방장이 나가면 WebSocket으로 `HOST_CHANGE` 이벤트가 브로드캐스트됩니다.  
> ℹ️ 마지막 플레이어가 나가면 방이 자동으로 삭제됩니다.

---

### 5. 게임 시작

> 방장이 게임을 시작합니다. 방장만 호출 가능합니다. 성공하면 모든 참가자에게 `GAME_START` WebSocket 이벤트가 전송됩니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/start`
- **Request Header**: `nickname: 방장닉네임` (**필수**, 방장 여부 서버에서 검증)
- **Request Body**: 없음

**Response `data`**: `null`

> ⚠️ 방장 닉네임이 일치하지 않으면 `G004` 에러가 반환됩니다.  
> ⚠️ 이미 게임 중이면 `G006` 에러가 반환됩니다.

---

## 🔌 WebSocket (STOMP)

> 실시간 게임 이벤트(채팅, 정답, 타이머, 점수 등)는 모두 WebSocket을 통해 처리됩니다.

### 연결 방법

```
WebSocket Endpoint: /ws-quiz
Protocol: STOMP over SockJS
```

**연결 예시 (JavaScript)**:
```javascript
const socket = new SockJS('/ws-quiz');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // 연결 성공 후 방 구독
  stompClient.subscribe('/subscribe/room/{roomId}', (message) => {
    const event = JSON.parse(message.body);
    // event.type으로 분기 처리
  });
});
```

---

### 📤 클라이언트 → 서버 (메시지 전송)

#### 채팅 / 정답 입력

> 채팅 메시지를 전송합니다. 게임 중에는 정답 확인도 함께 처리됩니다 (별도 정답 입력 API 없음, 채팅 = 정답 입력).

- **Destination**: `/publish/room/{roomId}/chat`
- **Header**: `nickname: 닉네임` (**필수**)
- **Payload**: `String` (채팅 내용 또는 정답)

**전송 예시**:
```javascript
stompClient.send(
  `/publish/room/${roomId}/chat`,
  { nickname: '내닉네임' },
  '노래 제목 정답'
);
```

---

### 📥 서버 → 클라이언트 (이벤트 수신)

> 구독 경로: `/subscribe/room/{roomId}`  
> 모든 이벤트는 `type` 필드로 구분합니다.

```javascript
stompClient.subscribe(`/subscribe/room/${roomId}`, (message) => {
  const event = JSON.parse(message.body);
  
  switch (event.type) {
    case 'PLAYER_JOIN':    // 플레이어 입장
    case 'PLAYER_LEAVE':   // 플레이어 퇴장
    case 'HOST_CHANGE':    // 방장 변경
    case 'CHAT':           // 채팅 메시지
    case 'GAME_START':     // 게임 시작
    case 'TIMER_TICK':     // 타이머 카운트다운
    case 'CORRECT_ANSWER': // 정답 맞힘
    case 'ROUND_TIMEOUT':  // 라운드 종료
    case 'GAME_END':       // 게임 최종 종료
  }
});
```

---

#### ① PLAYER_JOIN - 플레이어 입장

> 새로운 플레이어가 방에 입장했을 때 발생합니다. 참가자 목록 UI를 업데이트하세요.

```json
{
  "type": "PLAYER_JOIN",
  "nickname": "입장한플레이어닉네임"
}
```

---

#### ② PLAYER_LEAVE - 플레이어 퇴장

> 플레이어가 방에서 나갔을 때 발생합니다. 참가자 목록에서 해당 플레이어를 제거하세요.

```json
{
  "type": "PLAYER_LEAVE",
  "nickname": "퇴장한플레이어닉네임"
}
```

---

#### ③ HOST_CHANGE - 방장 변경

> 방장이 나가서 방장 권한이 다른 플레이어로 이전될 때 발생합니다. 새 방장에게는 게임 시작 버튼을 표시하세요.

```json
{
  "type": "HOST_CHANGE",
  "newHost": "새방장닉네임"
}
```

---

#### ④ CHAT - 일반 채팅

> 누군가 채팅 메시지를 전송했을 때 발생합니다. 게임 중에는 오답도 채팅으로 표시됩니다.

```json
{
  "type": "CHAT",
  "nickname": "닉네임",
  "message": "채팅 내용"
}
```

---

#### ⑤ GAME_START - 게임 시작

> 방장이 게임 시작 버튼을 눌렀을 때 모든 참가자에게 발생합니다. 대기 화면에서 게임 화면으로 전환하세요.

```json
{
  "type": "GAME_START",
  "songCount": 10
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `songCount` | `int` | 이번 게임에서 출제될 총 곡 수 |

---

#### ⑥ TIMER_TICK - 타이머 카운트다운

> 현재 라운드의 남은 시간을 1초마다 전송합니다. 화면에 타이머를 표시하세요.

```json
{
  "type": "TIMER_TICK",
  "remainingSeconds": 25
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `remainingSeconds` | `int` | 현재 라운드 남은 초 |

---

#### ⑦ CORRECT_ANSWER - 정답 맞힘

> 누군가 정답을 맞혔을 때 발생합니다. 정답자와 획득 점수를 표시하고, 라운드 종료를 준비하세요.

```json
{
  "type": "CORRECT_ANSWER",
  "nickname": "정답자닉네임",
  "answer": "정답곡제목",
  "score": 110
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `nickname` | `String` | 정답을 맞힌 플레이어 닉네임 |
| `answer` | `String` | 정답 내용 (곡 제목 등) |
| `score` | `int` | 해당 라운드에서 획득한 점수 |

---

#### ⑧ ROUND_TIMEOUT - 라운드 종료 / 다음 곡 준비

> 라운드 시간이 종료되거나 정답이 나와 다음 라운드로 넘어갈 때 발생합니다.

```json
{
  "type": "ROUND_TIMEOUT",
  "nextSongIndex": 2
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `nextSongIndex` | `int` | 다음에 재생될 곡의 인덱스 (0부터 시작) |

> ℹ️ `nextSongIndex`가 `songCount`보다 크거나 같으면 `GAME_END` 이벤트가 뒤따라 옵니다.

---

#### ⑨ GAME_END - 게임 최종 종료

> 모든 라운드가 끝났을 때 발생합니다. 최종 점수 화면으로 전환하세요.

```json
{
  "type": "GAME_END",
  "rankings": {
    "플레이어1": 300,
    "플레이어2": 150,
    "플레이어3": 80
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `rankings` | `Map<String, Integer>` | 닉네임: 최종점수 쌍의 객체 |

---

## ❗ 에러 코드

| 코드 | 설명 | HTTP 상태 | 발생 상황 |
|------|------|-----------|-----------|
| `G001` | 방 인원 초과 | 400 | 방 입장 시 최대 인원 초과 |
| `G002` | 방을 찾을 수 없음 | 400 | 존재하지 않는 `roomId`로 요청 |
| `G003` | 방이 비어있음 | 400 | 플레이어 없이 게임 시작 시도 |
| `G004` | 방장 권한 없음 | 403 | 방장이 아닌 사람이 게임 시작 시도 |
| `G005` | 분산 락 획득 실패 | 500 | 동시에 여러 요청이 몰렸을 때 (재시도 권장) |
| `G006` | 이미 게임 진행 중 | 400 | 이미 시작된 게임을 다시 시작 시도 |
| `E500` | 서버 내부 오류 | 500 | 서버 예외 상황 |

**에러 응답 예시**:
```json
{
  "result": "FAIL",
  "data": null,
  "error": {
    "code": "G002",
    "message": "방을 찾을 수 없습니다."
  }
}
```

---

## 🗺️ 전체 게임 플로우

```
1. [로비] GET /game/rooms           → 방 목록 조회
2. [방 만들기] POST /game/rooms      → 방 생성 → roomId 획득
3. [방 입장] POST /game/rooms/{roomId}/join (+ nickname 헤더)
4. [WebSocket 연결] /ws-quiz 연결 + /subscribe/room/{roomId} 구독
5. [대기] PLAYER_JOIN / PLAYER_LEAVE / CHAT 이벤트 수신
6. [게임 시작] 방장 → POST /game/rooms/{roomId}/start
7. [게임 중] GAME_START → TIMER_TICK → (CHAT / CORRECT_ANSWER) → ROUND_TIMEOUT → 반복
8. [게임 종료] GAME_END 이벤트 수신 → 결과 화면 표시
9. [퇴장] POST /game/rooms/{roomId}/leave
```
