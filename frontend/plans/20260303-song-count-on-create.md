# 방 생성 시 songCount 추가 계획

## 목표
방 생성 폼에서 "방장(자동 설정)" 칸을 **곡 개수 선택**으로 교체하고,
방 생성 API 요청에 `songCount` 필드를 함께 전송한다.

---

## 변경 파일

### `src/components/RoomList.tsx`
- `songCount` 상태 추가 (`useState(10)`, 기본값 10)
- 방 생성 폼의 **"방장 (자동 설정)"** `<input readonly>` 칸 → **"곡 수 선택"** `<select>`로 교체
  - 선택지: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
- `생성` 버튼 클릭 시 `onCreateRoom(title, maxPlayers, category, songCount)` 호출에 `songCount` 추가

### `src/pages/RoomListPage.tsx`
- `onCreateRoom` prop 시그니처 확인 및 `songCount` 파라미터 추가

### `src/hooks/useGameLogic.ts`
- `createRoom(title, maxPlayers, category, songCount)` 시그니처에 `songCount: number` 추가
- API 요청 body에 `songCount` 필드 추가
  ```
  POST /game/rooms
  body: { title, maxPlayers, hostName, category, songCount }
  ```

## 검증
- 방 생성 폼에서 곡 수 선택 드롭다운이 표시되는지 확인
- 방 생성 요청 payload에 `songCount`가 포함되는지 확인
