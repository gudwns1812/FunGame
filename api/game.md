# 게임 제어 API (game.md)

이 문서는 게임의 시작, 스킵, 실시간 상태 조회와 관련된 HTTP API를 정의합니다.

## 1. 게임 시작

방장이 게임을 시작합니다. 게임이 시작되면 WebSocket을 통해 모든 플레이어에게 `GAME_START` 이벤트가 전송됩니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/start`
- **Header**: `playerName: 방장닉네임` (필수)
- **권한**: 요청을 보낸 플레이어가 해당 방의 방장이어야 합니다.
- **Response Data (data 필드)**: `null`

## 2. 퀴즈 스킵 투표

현재 진행 중인 퀴즈를 건너뛰기 위해 투표합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/skip`
- **Header**: `playerName: 플레이어닉네임` (필수)
- **Response Data (data 필드)**: `null`

## 3. 실시간 플레이어 랭킹 조회

게임 진행 중에 실시간 플레이어 점수 및 순위를 조회합니다.

- **Method**: `GET`
- **Path**: `/game/rooms/{roomId}/play/rank`
- **Response Data (data 필드)**: `List<PlayerScore>`

### PlayerScore 객체
| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`nickname`** | `String` | 플레이어 닉네임. |
| **`score`** | `int` | 플레이어의 현재 점수. |

## 4. 범용 게임 액션 수행

보드게임(할리갈리 등)에서 카드 뒤집기, 종 울리기 등의 특정 액션을 수행합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/action`
- **Header**: `Content-Type: application/json`
- **Request Body**: `GameAction`

### GameAction 객체
| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`playerName`** | `String` | 액션을 수행하는 플레이어 닉네임. |
| **`type`** | `String` | 액션 종류 (`FLIP_CARD`, `PRESS_BELL`, `SUBMIT_ANSWER`, `SKIP_VOTE`). |
| **`value`** | `String` | 액션에 필요한 추가 값 (필요 시). |

- **Response Data (data 필드)**: `null` (성공 시 200 OK)

---
*참고: 게임 진행 중의 실시간 데이터(타이머, 정답 확인 등)는 `api/websocket.md`를 참고하세요.*
