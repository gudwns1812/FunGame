# 전적 API (record.md)

이 문서는 로그인한 회원이 자기 전적 요약을 읽는 HTTP API를 정의합니다. 실제 백엔드 `GameRecordController` 및 `GameRecordService` 와 동기화되었습니다.

## 1. 내 전적 요약

- **Method**: `GET`
- **Path**: `/api/records/mine`
- **인증**: 필요 (자기 전적만 읽을 수 있습니다)
- **Response Data (data 필드)**: `Array<GameRecord>` — 언제나 노래 퀴즈 · CS 퀴즈 · 행맨 순서의 세 줄

### GameRecord

| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`gameType`** | `String` | `SONG` / `CS` / `HANGMAN` |
| **`played`** | `Number` | 그 게임에서 기록된 판 수. 도중에 나간 판도 들어갑니다. |
| **`won`** | `Number` | 이긴 판 수 |
| **`lost`** | `Number` | 진 판 수. 언제나 `played - won` 입니다. |
| **`winRate`** | `Number \| null` | 반올림한 승률(%). 한 판도 하지 않았으면 `null` 입니다. |

### 게임 종류를 합치지 않는다

게임마다 승패 규칙이 달라서(노래 · CS 는 경쟁, 행맨은 협동) 합친 숫자는 뜻이 없습니다. 응답에 합계는 없고, 화면도 게임별로 따로 보여 줍니다. 한 판도 하지 않은 게임도 0 으로 빠지지 않고 내려갑니다.

### 무엇이 기록되나

| 게임 | 승리 | 패배 |
| :--- | :--- | :--- |
| 노래 퀴즈 · CS 퀴즈 | 끝까지 남은 1위 (공동 포함) | 나머지, 도중에 나간 사람. 1위가 0점이면 전원 |
| 행맨 | 성공했을 때 끝까지 남은 전원 | 실패했을 때 전원, 도중에 나간 사람 |

- 혼자 시작한 판은 기록하지 않습니다.
- 판이 결과를 낸 순간 기록되며, 같은 판은 한 번만 기록됩니다.

### 응답 예시

```json
{
  "result": "SUCCESS",
  "data": [
    { "gameType": "SONG", "played": 3, "won": 2, "lost": 1, "winRate": 67 },
    { "gameType": "CS", "played": 0, "won": 0, "lost": 0, "winRate": null },
    { "gameType": "HANGMAN", "played": 1, "won": 0, "lost": 1, "winRate": 0 }
  ],
  "error": null
}
```
