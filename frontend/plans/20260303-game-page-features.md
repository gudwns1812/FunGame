# 게임 페이지 기능 개선 계획

## 목표
인게임 화면(`Game.tsx`)에 아래 3가지 기능을 추가한다.

1. **채팅 로그 & 입력바 가로 확장** - 현재 좌측 컬럼 안에만 있는 채팅 영역을 더 넓게 표시
2. **랭킹 초기 로드** - 페이지 진입 시 `GET /game/rooms/{roomId}/play/rank`를 호출해 응답(`player: String, score: int`) 배열을 우측 점수판에 반영
3. **엔터키 → 채팅 입력창 포커스** - 화면 어디서든 엔터를 누르면 하단 정답 입력창으로 커서가 이동
4. **자신의 플레이어 순번 저장** - status가 PLAYING으로 바뀔 때 현재 플레이어 목록에서 자신의 인덱스(1-based)를 `playerIndex` 상태로 저장; PLAYING에서 다른 status로 바뀌면 `playerIndex`를 초기화(null)

---

## 변경 파일

### `src/hooks/useGameLogic.ts`
- `roomId`를 훅 반환값에 추가 (현재 내부 state지만 외부 미노출)
- `fetchRank` 함수 추가
  ```
  GET /game/rooms/{roomId}/play/rank
  headers: { playerName: encodeURIComponent(nickname) }
  응답: { result: 'SUCCESS', data: [{ player, score }, ...] }
  → setPlayers() 로 랭킹 반영
  ```
  - **에러 발생 시**: `setStatus('ROOM_LIST')` 호출 → App.tsx의 라우팅에 의해 `/rooms`로 자동 이동
- **페이지 재진입(새로고침) 처리**: 마운트 시 `status === 'PLAYING'` 이고 `roomId`가 있는 경우, 백엔드에 비동기(fire-and-forget)로 `POST /game/rooms/{roomId}/join` 요청 전송 (응답 결과 무관하게 무시, 이미 PLAYING 상태 유지)
- 반환 객체에 `roomId`, `fetchRank`, `playerIndex` 포함
- **`playerIndex` 상태 추가** (`useState<number | null>(null)`)
  - `handleEvent`의 `GAME_START` 케이스에서 `setStatus('PLAYING')` 와 함께, 현재 `players` 배열에서 `nickname`과 일치하는 항목의 인덱스(1-based)를 찾아 `setPlayerIndex(idx)` 저장
  - `setStatus`가 `PLAYING` 이외의 값으로 호출될 때마다 `setPlayerIndex(null)` 로 초기화
    - 대상 위치: `leaveRoom`, `fetchRank` 에러 처리, `GAME_END` 이벤트 처리 등 status를 바꾸는 모든 곳

### `src/App.tsx`
- `useGameLogic`에서 `roomId`, `fetchRank` 구조분해
- `<GamePage>`에 `roomId`와 `onFetchRank={fetchRank}` prop 전달

### `src/pages/GamePage.tsx`
- `roomId: string`, `onFetchRank: () => Promise<void>` prop 추가
- `<Game>` 컴포넌트에 두 prop 전달

### `src/components/Game.tsx`
- **prop 추가**: `roomId`, `onFetchRank`
- **랭킹 초기 로드**: `useEffect(() => { onFetchRank(); }, [])` — 마운트 시 1회 호출
- **엔터키 포커스**: `useEffect`로 `window keydown` 이벤트 등록 → `key === 'Enter'` 이면 `inputRef.current?.focus()`
  - 단, 이미 입력창에 포커스된 상태면 폼 제출로 동작하도록 분기 없이 자연스럽게 처리
- **채팅 UI 가로 확장**: 현재 좌측 컬럼(`flex-1`) 안의 채팅 패널이 유튜브 영역 아래에 세로로 배치되어 있음
  - 전체 레이아웃을 `flex-col` 기반으로 재구성하여 채팅 영역이 전체 너비를 사용하도록 변경
  - 구체적으로: 상단(타이머) → 중단(YouTube + 스코어보드 가로 배치) → 하단(채팅 로그 + 입력바 전체 너비) 구조로 변경
