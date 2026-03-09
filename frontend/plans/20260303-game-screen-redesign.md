# 게임 화면 전면 개편 계획

## 목표
레이아웃 재구성, 이벤트 타입 변경, 채팅 로그 초기화 로직 추가

---

## 변경 파일

### `src/types/game.ts`

기존 이벤트 타입을 아래와 같이 교체/추가한다.

| 기존 | 변경 |
|------|------|
| `GAME_START: { songCount }` | `GAME_START: { gameType, category, songCount, message }` |
| `ROUND_TIMEOUT: { nextSongIndex }` | `ROUND_END: { answer: string; winner: string \| null }` |
| `GAME_END: { rankings }` | `GAME_RESULT: { rankings: Record<string, number> }` |
| _(없음)_ | `ROUND_START: { videoId: string; roundIndex: number }` 추가 |

---

### `src/hooks/useGameLogic.ts`

#### 상태 추가/변경
- `roundEndInfo: { answer: string; winner: string | null } | null` 상태 추가
- `gameStartInfo: { gameType: string; category: string; songCount: number; message: string } | null` 상태 추가
- `setLogs` 공개 (또는 `clearLogs` 콜백 추가) → Game 컴포넌트가 로그를 직접 비울 수 있도록

#### handleEvent 이벤트 처리 변경
| 이벤트 | 기존 | 변경 |
|--------|------|------|
| `GAME_START` | `setStatus('PLAYING')` | `setGameStartInfo({...})` 저장, **status는 변경하지 않음** (PLAYING은 ROUND_START에서 처리), `setLogs([])` 초기화 |
| `ROUND_START` | _(없음)_ | `setStatus('PLAYING')`, `setCurrentVideoId(event.videoId)`, `setRoundEndInfo(null)`, `setLogs([])` 초기화 |
| `ROUND_END` | `ROUND_TIMEOUT` 처리 | `setRoundEndInfo({ answer, winner })` |
| `GAME_RESULT` | `GAME_END` → `setStatus('RESULT')` | `setStatus('RESULT')`, `setPlayerIndex(null)`, `setLogs([])` 초기화 |

- `currentVideoId`를 `useState`가 아닌 `useState`로 바꾸되 setter를 내부에서 사용 가능하도록
- 반환 객체에 `roundEndInfo`, `gameStartInfo` 포함

#### 409 처리 (이전 플랜에서 삭제된 항목 재포함)
- PLAYING 재진입 join `.catch()`에서 `error.response?.status === 409` 이면 무시, 나머지만 ROOM_LIST 전환

---

### `src/App.tsx`
- `useGameLogic`에서 `roundEndInfo`, `gameStartInfo` 구조분해하여 `GamePage`에 전달

### `src/pages/GamePage.tsx`
- `roundEndInfo`, `gameStartInfo` prop 추가 및 `Game`에 전달

### `src/components/Game.tsx` (UI 전면 개편)

#### 새 레이아웃 (가로 2컬럼 + 우측 수직 스택)

```
┌─────────────────────────────────────────────────┐
│  [좌: 랭킹 패널]   │  [우: 수직 스택]           │
│                    │  ┌──────────────────────┐  │
│  #1 플레이어  100  │  │    타이머 게이지      │  │
│  #2 플레이어   80  │  ├──────────────────────┤  │
│  ...               │  │  노래 정보 (큰 패널) │  │
│                    │  │  - 라운드 중: 분석중 │  │
│                    │  │  - GAME_START: 게임  │  │
│                    │  │    정보 표시         │  │
│                    │  │  - ROUND_END: answer │  │
│                    │  │    + winner 표시     │  │
│                    │  ├──────────────────────┤  │
│                    │  │    채팅 로그          │  │
│                    │  ├──────────────────────┤  │
│                    │  │    채팅 입력바        │  │
│                    │  └──────────────────────┘  │
└─────────────────────────────────────────────────┘
```

#### prop 추가
- `roundEndInfo: { answer: string; winner: string | null } | null`
- `gameStartInfo: { gameType: string; category: string; songCount: number; message: string } | null`

#### 노래 정보 패널 표시 규칙
| 상태 | 표시 내용 |
|------|----------|
| `gameStartInfo` 있고 라운드 미시작 | gameType, category, songCount, message 표시 |
| 라운드 진행 중 (roundEndInfo 없음) | "노래 분석 중..." 애니메이션 |
| `roundEndInfo` 있음 | 정답: `answer` 표시, `winner`가 있으면 "정답자: {winner}" 추가 표시 |

---

## 검증 계획

### 타입 검증
```
npx tsc --noEmit
```

### 수동 검증
직접 서버와 연결 후 아래 이벤트 흐름 확인:
1. 방 생성 → GAME_START 수신 → 노래 정보 패널에 게임 정보 표시 확인
2. ROUND_START 수신 → 게임 시작, 노래 정보 초기화, 로그 초기화 확인
3. ROUND_END 수신 → 노래 정보 패널에 answer + winner 표시 확인
4. GAME_RESULT 수신 → 결과 화면 전환 확인
5. 새로고침 후 409 에러 시 게임 화면 유지 확인
