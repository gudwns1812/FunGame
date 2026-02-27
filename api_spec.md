# Song Quiz API Specification

## 1. REST API (HTTP)

### 공통 응답 구조 (ApiResponse)
모든 API 요청에 대한 응답은 아래 형식을 따릅니다.

```json
{
  "result": "SUCCESS" | "FAIL",
  "data": Object | null,
  "error": {
    "code": "String",
    "message": "String"
  } | null
}
```

---

### 1.1 방 목록 조회
*   **Method**: `GET`
*   **Path**: `/game/rooms`
*   **Response Data**: `List<RoomInfo>`
    ```json
    [
      {
        "roomId": "1",
        "title": "K-POP 퀴즈방",
        "hostName": "방장닉네임",
        "maxPlayers": 8,
        "currentPlayers": 3
      }
    ]
    ```

### 1.2 방 생성
*   **Method**: `POST`
*   **Path**: `/game/rooms`
*   **Request Body**:
    ```json
    {
      "title": "방 제목",
      "maxPlayers": 8,
      "hostName": "방장닉네임",
      "category": "KPOP" | "POP" | "BALLADE" | "RAP" | "OST"
    }
    ```
*   **Response Data**: `String` (생성된 방의 ID)

### 1.3 방 입장
*   **Method**: `POST`
*   **Path**: `/game/rooms/{roomId}/join`
*   **Header**: `nickname: 플레이어닉네임` (필수)
*   **Response Data**: `null`

### 1.4 방 퇴장
*   **Method**: `POST`
*   **Path**: `/game/rooms/{roomId}/leave`
*   **Header**: `nickname: 플레이어닉네임` (필수)
*   **Response Data**: `null`

### 1.5 게임 시작
*   **Method**: `POST`
*   **Path**: `/game/rooms/{roomId}/start`
*   **Header**: `nickname: 방장닉네임` (방장 권한 확인 필수)
*   **Response Data**: `null`

---

## 2. WebSocket (STOMP)

### Connection Endpoint
*   **URL**: `/ws-quiz`
*   **Protocol**: SockJS 지원

### Message Broker
*   **Subscribe Prefix**: `/subscribe`
*   **Publish Prefix**: `/publish`

---

### 2.1 클라이언트 송신 (Client to Server)

#### 채팅 및 정답 입력
*   **Destination**: `/publish/room/{roomId}/chat`
*   **Header**: `nickname: 닉네임`
*   **Payload**: `String` (채팅 메시지)

---

### 2.2 서버 브로드캐스트 (Server to Client)
모든 메시지는 `/subscribe/room/{roomId}` 경로를 구독 중인 사용자들에게 전달됩니다.

#### 공통 메시지 구조
```json
{
  "type": "EVENT_TYPE",
  "...": "Data Fields"
}
```

#### EVENT_TYPE 목록:

1.  **PLAYER_JOIN** (플레이어 입장)
    ```json
    { "type": "PLAYER_JOIN", "nickname": "닉네임" }
    ```
2.  **PLAYER_LEAVE** (플레이어 퇴장)
    ```json
    { "type": "PLAYER_LEAVE", "nickname": "닉네임" }
    ```
3.  **HOST_CHANGE** (방장 변경)
    ```json
    { "type": "HOST_CHANGE", "newHost": "새방장닉네임" }
    ```
4.  **CHAT** (일반 채팅)
    ```json
    { "type": "CHAT", "nickname": "닉네임", "message": "채팅내용" }
    ```
5.  **GAME_START** (게임 시작)
    ```json
    { "type": "GAME_START", "songCount": 10 }
    ```
6.  **TIMER_TICK** (카운트다운)
    ```json
    { "type": "TIMER_TICK", "remainingSeconds": 25 }
    ```
7.  **CORRECT_ANSWER** (정답 발생)
    ```json
    { 
      "type": "CORRECT_ANSWER", 
      "nickname": "정답자", 
      "answer": "정답문구", 
      "score": 110 
    }
    ```
8.  **ROUND_TIMEOUT** (라운드 종료/다음 곡 준비)
    ```json
    { "type": "ROUND_TIMEOUT", "nextSongIndex": 2 }
    ```
9.  **GAME_END** (게임 최종 종료)
    ```json
    { 
      "type": "GAME_END", 
      "rankings": {
        "플레이어1": 150,
        "플레이어2": 100
      } 
    }
    ```

---

## 3. 에러 코드 (ErrorCode)

| 코드 | 설명 | HTTP 상태 |
| :--- | :--- | :--- |
| **G001** | 방 인원 초과 | 400 Bad Request |
| **G002** | 방을 찾을 수 없음 | 400 Bad Request |
| **G003** | 방이 비어있음 | 400 Bad Request |
| **G004** | 방장 권한 없음 | 403 Forbidden |
| **G005** | 분산 락 획득 실패 | 500 Internal Server Error |
| **G006** | 이미 게임이 진행 중 | 400 Bad Request |
| **E500** | 서버 내부 오류 | 500 Internal Server Error |
