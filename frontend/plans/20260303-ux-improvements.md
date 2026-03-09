# UI/UX 개선 3가지 계획

## 1. 방 생성 후 즉시 페이지 이동

### 원인
`createRoom`에서 `setStatus('WAITING')`을 호출한 후 `connectWebSocket()`을 비동기로 호출하는데, WebSocket 연결 완료를 기다리는 동안 0.5초 정도 로비에 머무는 것처럼 보임.
또한 `isBootstrapping` 로직이 `WAITING` 상태를 마운트 시에만 처리하므로, 방 생성 방향은 영향 없음. **실제 원인은 React 상태 업데이트 batching / 라우터 이동 타이밍.**

### 수정: `useGameLogic.ts`
`createRoom`에서 WebSocket 연결 **전에** 상태 변경:
```typescript
setRoomId(newRoomId);
setIsHost(true);
setPlayers([...]);
setStatus('WAITING');      // ← 먼저 상태 변경 (라우터 이동 트리거)
connectWebSocket(newRoomId); // ← 그 다음 연결
```
이미 이 순서이므로, 추가로 `window.history.pushState` 제거 (router가 자동으로 처음)하거나 `setStatus` 타이밍을 확인.

**실제 수정**: `createRoom` / `joinRoom` 성공 시 `addLog` → `connectWebSocket` 순서를 `setStatus` 바로 뒤로 재배치해 렌더링이 먼저 일어나도록 정리.

---

## 2. 대기실 로비에서 닉네임 변경 기능

### 수정: `useGameLogic.ts`
`changeNickname` 함수 추가:
```typescript
const changeNickname = useCallback(async (newName: string) => {
  // 서버에 닉네임 등록 (enterLobby와 동일 방식)
  localStorage.setItem('ums_nickname', newName);
  setNickname(newName);
}, []);
```
반환 객체에 `changeNickname` 포함.

### 수정: `RoomList.tsx`
- 헤더 영역에 **"닉네임 변경"** 버튼 추가
- 클릭 시 인라인 폼 (input + 확인/취소) 표시
- 확인 시 `onChangeNickname(newName)` 호출

### 수정: `RoomListPage.tsx`
- `onChangeNickname` prop 추가

### 수정: `App.tsx`
- `changeNickname` 구조분해 후 `RoomListPage`에 전달

---

## 3. 채팅 로그에서 시스템 메시지 제거 (Join/Leave만 유지)

### 현재 상황
`addLog('[시스템] ...')` 가 게임 시작, 방 입장, 정답, 라운드 종료 등 모든 시스템 이벤트에 사용됨.

### 수정: `useGameLogic.ts`
`addLog` 호출 중 **채팅 로그에서 제거**할 항목들 (남길 것: PLAYER_JOIN / PLAYER_LEAVE 관련만, 또는 아예 CHAT만):
- GAME_START, ROUND_START, ROUND_END, CORRECT_ANSWER, GAME_RESULT 등의 `addLog` 호출 제거
- HOST_CHANGE `addLog` 제거
- PLAYER_JOIN / PLAYER_LEAVE는 `fetchRoomUsers`에서 처리하므로 로그 없이도 UI에 반영됨 → 모든 `addLog('[시스템]...')` 제거, 순수 CHAT 이벤트 로그만 유지

> console.log는 유지해도 됨 (디버그용)

---

## 4. 노래 정보 패널 하단에 현재/전체 라운드 상시 표시

### 수정: `types/game.ts`
`ROUND_START` 타입에 `currentRound`, `totalRound` 필드 추가:
```typescript
| { type: 'ROUND_START'; videoURL: string; roundIndex: number; currentRound: number; totalRound: number }
```

### 수정: `useGameLogic.ts`
- `currentRound`, `totalRound` 상태 추가 (`useState<number>(0)`)
- `ROUND_START` 핸들러에서 `setCurrentRound(event.currentRound)`, `setTotalRound(event.totalRound)` 저장
- 반환 객체에 포함

### 수정: `App.tsx` → `GamePage.tsx` → `Game.tsx`
- `currentRound`, `totalRound` prop 체인 전달
- `Game.tsx` 노래 정보 패널 **하단**에 항상 표시:
  ```tsx
  {/* 패널 최하단 고정 */}
  {currentRound > 0 && (
    <p>ROUND {currentRound} / {totalRound}</p>
  )}
  ```

---

## 5. 창 닫기 / 새로고침 시 PLAYING 유지

### 원인
현재 `popstate` 이벤트에서 `PLAYING` 상태일 때도 `leaveRoom()`을 호출함.
SPA에서 뒤로가기를 누르면 popstate가 발생 → `leaveRoom()` → localStorage `ums_status = 'ROOM_LIST'`로 변경.
새로고침 시 bootstrap이 join 실패하면 ROOM_LIST로 전환되는 것도 공범.

### 수정: `useGameLogic.ts` — popstate 핸들러
`PLAYING` 상태는 popstate에서 leaveRoom 호출 제외:
```typescript
const handlePopState = () => {
  // PLAYING 중에는 뒤로가기 무시 (게임 중 이탈 방지)
  if (status === 'WAITING' || status === 'RESULT') {
    leaveRoom();
  }
  // PLAYING은 처리 안 함 → localStorage 유지 → 새로고침처럼 동작
};
```

---

## 변경 파일 요약

| 파일 | 변경 |
|------|------|
| `useGameLogic.ts` | `changeNickname` 추가, 시스템 addLog 전면 제거, `currentRound`/`totalRound` 상태 추가, popstate에서 PLAYING 제외, 반환 포함 |
| `types/game.ts` | `ROUND_START`에 `currentRound`, `totalRound` 추가 |
| `App.tsx` | `changeNickname`, `currentRound`, `totalRound` 전달 |
| `RoomListPage.tsx` | `onChangeNickname` prop 추가 |
| `RoomList.tsx` | 닉네임 변경 UI 추가 |
| `GamePage.tsx` / `Game.tsx` | `currentRound`, `totalRound` prop 추가, 노래 정보 패널 하단 고정 표시 |
