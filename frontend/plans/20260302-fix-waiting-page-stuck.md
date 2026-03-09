# Waiting 페이지에서 뒤로가기/나가기 불가 버그 수정 계획

## 배경 / 문제
- "나가기" 버튼을 눌러도 `/waiting` 페이지에 계속 머무는 현상 발생.

## 원인 분석

### 현재 `leaveRoom` 코드
```typescript
const leaveRoom = useCallback(async () => {
  if (!roomId) return;  // ← roomId가 없으면 바로 종료
  try {
    await axios.post(`/game/rooms/${roomId}/leave`, ...);
    setRoomId(null);
    setStatus('ROOM_LIST');  // ← 성공 시에만 상태 변경
    ...
  } catch (error) {
    console.error('Leave room failed:', error);
    // ← 실패 시 아무것도 하지 않음! status가 WAITING으로 유지됨
  }
}, [roomId, nickname, addLog]);
```

### 문제점 2가지
1. **API 실패 시 탈출 불가**: leave API가 실패(네트워크 오류, 이미 나간 상태 등)하면 catch에서 아무것도 하지 않아 `status`가 영원히 `WAITING`으로 남음.
2. **`roomId` 없을 때 탈출 불가**: `roomId`가 null인 상태에서 나가기를 시도하면 `return`되어 `status`가 바뀌지 않음. (localStorage 불일치 시 발생 가능)

## 변경 계획

### `src/hooks/useGameLogic.ts` - `leaveRoom` 수정
- API 성공 여부와 **무관하게** 항상 클라이언트 상태를 초기화 (`setRoomId(null)`, `setStatus('ROOM_LIST')`, `setPlayers([])`, `setIsHost(false)`) 처리.
- `roomId`가 null이어도 `status`가 `WAITING`/`PLAYING`/`RESULT`라면 상태를 초기화하도록 수정.

```typescript
// 변경 후
const leaveRoom = useCallback(async () => {
  // API 호출은 시도하되, 결과와 관계없이 클라이언트 상태는 항상 초기화
  if (roomId) {
    try {
      await axios.post(`/game/rooms/${roomId}/leave`, null, {
        headers: { nickname: encodeURIComponent(nickname) }
      });
    } catch (error) {
      console.error('Leave room failed:', error);
    }
  }
  if (stompClient.current) {
    stompClient.current.deactivate();
  }
  setRoomId(null);
  setStatus('ROOM_LIST');
  setPlayers([]);
  setIsHost(false);
  addLog('[시스템] 방에서 퇴장했습니다.');
}, [roomId, nickname, addLog]);
```

## 테스트 시나리오
1. 대기실에서 "나가기" 버튼 클릭 → `/rooms` 페이지로 이동 확인
2. 서버가 응답 없을 때(네트워크 차단) 나가기 클릭 → 그래도 `/rooms`로 이동 확인
3. 브라우저 뒤로가기로 대기실 탈출 시에도 정상 동작 확인

## 작업 범위
- `src/hooks/useGameLogic.ts` (leaveRoom 함수만 수정)
