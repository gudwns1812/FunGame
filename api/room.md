# 방 관리 API (room.md)

이 문서는 게임 방의 생성, 조회, 입장, 퇴장과 관련된 HTTP API를 정의합니다. 실제 백엔드 `GameController` 및 DTO 구조와 동기화되었습니다.

## 1. 방 목록 조회

현재 생성된 모든 게임 방의 목록을 조회합니다.

- **Method**: `GET`
- **Path**: `/game/rooms`
- **Response Data (data 필드)**: `List<RoomInfo>`

### RoomInfo 객체 (Record)
| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`roomId`** | `Long` | 방 고유 식별자. |
| **`title`** | `String` | 방 제목. |
| **`hostName`** | `String` | 방장 닉네임. |
| **`status`** | `String` | 방 상태 (`WAITING`, `PLAYING`). |
| **`maxPlayers`** | `int` | 방의 최대 수용 인원. |
| **`currentPlayers`** | `int` | 현재 접속 중인 플레이어 수. |

---

## 2. 방 생성

새로운 게임 방을 생성합니다.

- **Method**: `POST`
- **Path**: `/game/rooms`
- **Request Body (CreateRoomRequest)**:
    ```json
    {
      "gameType": "SONG" | "CS",
      "title": "String",
      "maxPlayers": 8,
      "hostName": "String",
      "category": "String" | null,
      "totalRound": 10
    }
    ```
- **Response Data (data 필드)**: `Long` (생성된 방의 ID)

---

## 3. 방 입장

특정 방에 플레이어로 입장합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/join`
- **Header**: `playerName: 플레이어닉네임` (필수, `@NickNameDecoder`에 의해 자동 디코딩됨)
- **Response Data (data 필드)**: `int` (플레이어 순번/Sequence)

---

## 4. 방 퇴장

특정 방에서 플레이어가 나갑니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/leave`
- **Header**: `playerName: 플레이어닉네임` (필수)
- **Response Data (data 필드)**: `null`

---

## 5. 준비 상태 변경

현재 플레이어의 준비 상태를 토글합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/ready`
- **Header**: `playerName: 플레이어닉네임` (필수)
- **Response Data (data 필드)**: `null`

---
*참고: 모든 응답은 `api/common.md`에 정의된 공통 응답 구조를 따릅니다.*
