# 신고 API (report.md)

이 문서는 콘텐츠 오류와 서비스 문의를 접수하는 HTTP API를 정의합니다. 실제 백엔드 `ReportController` 및 `ReportService` 와 동기화되었습니다.

## 1. 신고 접수

- **Method**: `POST`
- **Path**: `/api/reports`
- **인증**: 필요 (로그인한 회원만 접수할 수 있습니다)
- **Response Data (data 필드)**: 없음 (`null`)

### 요청 바디

| 필드 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| **`source`** | `String` | O | `IN_GAME` / `LOBBY`. 신고를 보낸 자리. |
| **`roomId`** | `Long` | 조건부 | `IN_GAME` 이면 필수, `LOBBY` 면 `null`. 어긋나면 `C001`. |
| **`reason`** | `String` | O | `CONTENT_NOT_SHOWN` / `CONTENT_WRONG` / `HINT_WRONG` / `ANSWER_WRONG` / `ETC`. |
| **`detail`** | `String` | 조건부 | `ETC` 이면 필수. 비어 있으면 `R003`. |
| **`gameType`** | `String` | X | `SONG` / `CS` / `HANGMAN`. `LOBBY` 신고에서 사용자가 고른 게임 종류. |

### 게임 정보는 요청에 담지 않는다

**요청 바디에 게임 정보가 하나도 없다는 것이 이 API 의 설계입니다.** 클라이언트가 보내면 위조할 수 있고, 정답·힌트 원본은 애초에 클라이언트에 내려가면 안 되는 값입니다. 어느 방·몇 라운드·어떤 곡이었고 정답이 무엇이었는지는 서버가 세션에서 직접 읽어 채웁니다.

`source` 가 `IN_GAME` 이면 `gameType` 도 무시하고 서버가 아는 값을 씁니다.

### 접수 시점에 따라 담기는 범위

| 접수 시점 | 담기는 것 |
| :--- | :--- |
| 라운드 진행 중 | 게임 종류 · 카테고리 · 방 · 라운드 · 문제 · 정답 · 힌트 · 문제 식별자 |
| 게임 중이지만 라운드 시작 전 | 게임 종류 · 방 |
| 대기실 | 방의 게임 종류 · 방 |
| 로비 | 사용자가 고른 게임 종류 (없으면 아무것도) |

담기는 값은 **접수 시점의 스냅샷**입니다. 신고를 받아 `song_entity` 행을 고치더라도 신고에 남은 값은 바뀌지 않습니다.

### 요청 예시 — 게임 중

```json
{
  "source": "IN_GAME",
  "roomId": 42,
  "reason": "HINT_WRONG",
  "detail": null,
  "gameType": null
}
```

### 요청 예시 — 로비

```json
{
  "source": "LOBBY",
  "roomId": null,
  "reason": "ETC",
  "detail": "로그인하면 가끔 튕겨요",
  "gameType": null
}
```

### 응답 예시

```json
{
  "result": "SUCCESS",
  "data": null,
  "error": null
}
```

---

## 2. 거절되는 경우

| 코드 | HTTP | 언제 |
| :--- | :--- | :--- |
| **`R001`** | 429 Too Many Requests | 한 회원이 1분 안에 접수 한도를 채웠습니다. |
| **`R002`** | 403 Forbidden | `roomId` 를 보냈지만 그 방에 참여 중이 아닙니다. 남의 방 상태를 긁어가는 통로가 되지 않게 막습니다. |
| **`R003`** | 400 Bad Request | `reason` 이 `ETC` 인데 `detail` 이 비어 있습니다. |
| **`C001`** | 400 Bad Request | `source` 와 `roomId` 가 어긋납니다 (`IN_GAME` 인데 방이 없거나, `LOBBY` 인데 방이 있음). |

---

## 3. 접수 뒤

- 신고는 `report` 테이블에 `OPEN` 상태로 저장됩니다.
- 저장한 뒤 비동기로 디스코드에 알립니다. 알림에는 신고 번호가 실려 관리자가 바로 그 건을 열 수 있습니다. **전송 실패는 접수 실패가 아닙니다** — 디스코드가 죽거나 rate limit 에 걸려도 신고는 남습니다.
- 같은 회원이 같은 문제를 같은 사유로 다시 신고하면 저장은 하고 알리지는 않습니다.

---

## 4. 내가 접수한 신고 조회

- **Method**: `GET`
- **Path**: `/api/reports/mine`
- **인증**: 필요 (자기 것만 돌려줍니다)
- **Response Data**: `List<MyReportResponse>` — 최근 접수 순

| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`id`** | `Long` | 신고 번호 |
| **`source`** | `String` | `IN_GAME` / `LOBBY` |
| **`reason`** | `String` | 사유 코드 |
| **`detail`** | `String` | 직접 작성한 내용 |
| **`gameType`** | `String` | 게임 종류 |
| **`status`** | `String` | `OPEN`(접수) / `RESOLVED`(처리 완료) |
| **`createdAt`** | `String` | 접수 시각 |
| **`comments[]`** | `Array` | 관리자 답변. `id` · `authorNickname` · `content` · `createdAt` |

### 신고자에게는 정답과 힌트를 내려주지 않는다

`report` 행에는 접수 시점의 **정답 · 힌트 · 유튜브 링크 · 문제 식별자 · 방 · 라운드**가 스냅샷으로 들어 있습니다. 이 응답에는 그것들이 **하나도 실리지 않습니다.**

내려주면 **라운드 진행 중에 신고 버튼을 눌러 정답을 읽는 치트 통로**가 되기 때문입니다. `ReportControllerDocsTest` 가 응답에 그 필드들이 없다는 것을 테스트로 고정합니다.

관리자 답변(`comments[].content`)은 신고자에게 그대로 보입니다. 답변에 정답을 옮겨 적으면 위의 방어가 무의미해지므로, 관리 화면이 입력창 옆에 그 사실을 적어 둡니다.

---

## 5. 문의 관리 (ADMIN 이상)

`/api/admin/**` 은 `SecurityConfig` 가 `hasAnyRole("ADMIN", "MASTER")` 로 막습니다.

### 5.1 전체 신고 조회

- **Method**: `GET`
- **Path**: `/api/admin/reports`
- **Query**: `status` (선택) — `OPEN` / `RESOLVED`. 없으면 전체
- **Response Data**: `List<AdminReportResponse>` — 최근 접수 순

`MyReportResponse` 의 모든 필드에 더해 신고자와 게임 컨텍스트 전부를 내려줍니다.

| 추가 필드 | 설명 |
| :--- | :--- |
| **`memberId`** · **`reporterNickname`** | 신고자 |
| **`quizCategory`** · **`contentId`** | 카테고리와 신고 대상 행의 식별자 |
| **`roomId`** · **`currentRound`** · **`totalRound`** | 접수 시점의 방과 라운드 |
| **`quizContent`** · **`quizAnswer`** · **`quizHint`** | 접수 시점의 문제 · 정답 · 힌트 |

### 5.2 답변 남기기

- **Method**: `POST`
- **Path**: `/api/admin/reports/{reportId}/comments`

```json
{ "content": "힌트를 고쳤습니다." }
```

답변은 관리자만 남깁니다. 신고자는 읽기만 하고, 더 할 말이 있으면 새로 접수합니다.
내용이 비어 있으면 `R004`, 없는 신고면 `R005` 입니다.

### 5.3 처리 상태 바꾸기

- **Method**: `PATCH`
- **Path**: `/api/admin/reports/{reportId}/status`

```json
{ "status": "RESOLVED" }
```

`OPEN` 과 `RESOLVED` 사이를 왕복할 수 있습니다. **왜 그렇게 처리했는지는 상태가 아니라 답변 문장이 말합니다.**
