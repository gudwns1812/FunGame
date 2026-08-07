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

## 3-1. 진행 중인 게임 상태 조회 (재입장용)

새로고침이나 연결 끊김으로 다시 들어온 클라이언트가 화면을 복원할 때 사용합니다.
웹소켓만 다시 연결하면 다음 라운드 전까지 현재 문제를 알 수 없으므로, 재연결 직전에 호출합니다.

- **Method**: `GET`
- **Path**: `/game/rooms/{roomId}/play/state`
- **Response Data (data 필드)**: `GameStateDto`

### GameStateDto 객체
| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`gameType`** | `String` | `SONG`, `CS`, `HANGMAN`. |
| **`category`** | `String` | 장르 또는 게임별 분류. |
| **`totalCount`** | `int` | 전체 문제 수. |
| **`currentRound`** | `int` | 현재 라운드. 첫 라운드 시작 전이면 `0`. |
| **`totalRound`** | `int` | 전체 라운드 수. |
| **`content`** | `String` | SONG/CS 용. `ROUND_START` 이벤트의 `content` 와 동일. 라운드 시작 전이면 `null`. |
| **`statusData`** | `List<String>` | HANGMAN 용. `HANGMAN_ACTION` 이벤트의 `status` 와 동일. |

> 진행 중인 방(`PLAYING`)의 `POST /join` 은 **해당 게임의 참가자였던 사람만** 성공합니다.
> 그 외에는 `이미 진행 중인 게임입니다.` 로 거절됩니다.

## 4. 범용 게임 액션 수행

게임 화면에서 정답 제출, 스킵 투표 등의 액션을 수행합니다.

- **Method**: `POST`
- **Path**: `/game/rooms/{roomId}/action`
- **Header**: `Content-Type: application/json`
- **Request Body**: `GameAction`

### GameAction 객체
| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`playerName`** | `String` | 액션을 수행하는 플레이어 닉네임. |
| **`type`** | `String` | 액션 종류 (`SUBMIT_ANSWER`, `SKIP_VOTE`). |
| **`value`** | `String` | 액션에 필요한 추가 값 (필요 시). |

- **Response Data (data 필드)**: `null` (성공 시 200 OK)

---
*참고: 게임 진행 중의 실시간 데이터(타이머, 정답 확인 등)는 `api/websocket.md`를 참고하세요.*
