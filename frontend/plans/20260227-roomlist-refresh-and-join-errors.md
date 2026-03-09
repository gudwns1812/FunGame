# 대기실 로비 새로고침 & 방 입장 오류 처리

## 요구사항 정리
- 로비(방 목록) 화면에 **새로고침 버튼**을 두고, 클릭 시 화면의 방 목록을 최신 상태로 갱신한다.
- 사용자가 방 카드를 클릭해 입장 시도할 때:
  - 이미 방이 없어졌거나(G002),
  - 인원이 가득 찼거나(G001),
  - 이미 게임이 진행 중이면(G006),
  서버 응답을 보고 **명확한 이유를 사용자에게 알려준다.**

## 현 구조 간단 정리
- 방 목록 UI: `src/components/RoomList.tsx`
- 로비 페이지 컨테이너: `src/pages/RoomListPage.tsx`
- 데이터/로직: `src/hooks/useGameLogic.ts`
  - 방 목록 조회: `fetchRooms()` (내부 useCallback + useEffect로 `status === 'ROOM_LIST'`일 때 자동 호출)
  - 방 입장: `joinRoom(room: Room)` → `POST /game/rooms/{roomId}/join`

## 구현 계획

### 1) 로비 새로고침 버튼
- 위치: `RoomList` 상단 헤더 오른쪽(“방 만들기” 버튼 옆 혹은 좌측)
- 동작:
  - `RoomList`에 `onRefreshRooms: () => void` props 추가.
  - `RoomListPage`에서 이 props로 `useGameLogic`의 `fetchRooms`를 대신 호출하는 핸들러를 넘김.
  - UI는 간단한 버튼(예: “새로고침” 텍스트 + 아이콘)으로 구현.
- 주의:
  - `fetchRooms`는 현재 hook 내부에만 쓰이므로, 반환값에 포함되도록 `useGameLogic`의 return 객체에 `fetchRooms`를 추가.

### 2) 방 입장 실패 케이스 처리
- 대상 함수: `useGameLogic.ts`의 `joinRoom(room: Room)`
- 변경 사항:
  - `axios.post` 실패 또는 `response.data.result === 'FAIL'`인 경우:
    - `const code = response.data?.error?.code`를 읽어서 분기.
    - 코드별 메시지:
      - `G001`: "해당 방은 인원이 가득 찼습니다."
      - `G002`: "해당 방을 찾을 수 없습니다. 이미 삭제되었을 수 있습니다."
      - `G006`: "이미 게임이 진행 중인 방입니다."
      - 그 외: "방에 입장할 수 없습니다. 잠시 후 다시 시도해주세요."
    - 간단하게는 `window.alert`로 사용자에게 알림.
    - 동시에 `addLog`로 시스템 로그도 남김(선택): 예) `[오류] (코드 G001) 방 인원이 가득 찼습니다.`
  - 성공 케이스 로직(현재 구현된 setRoomId/setStatus/connectWebSocket 등)은 그대로 유지.

### 3) UX 세부 사항
- 새로고침 버튼:
  - 클릭 시 짧게 로딩 상태를 표시할 수 있지만, 1차 구현에서는 그냥 즉시 `fetchRooms()`만 호출(추가 스피너는 추후 선택).
- 오류 알림:
  - 현재 별도의 토스트 시스템이 없으므로 **기본 alert** 사용.
  - 추후 공통 알림 컴포넌트가 생기면 그쪽으로 교체하기 쉽게, 메시지 생성은 `useGameLogic` 안에서 텍스트만 만들고, UI는 나중에 추상화 가능하도록 구성.

## 수정 예상 파일
- `src/hooks/useGameLogic.ts`
  - `joinRoom`의 에러 응답 처리 추가
  - `fetchRooms`를 반환 객체에 포함
- `src/components/RoomList.tsx`
  - `onRefreshRooms` props 추가
  - 상단 헤더에 새로고침 버튼 추가 및 핸들러 연결
- `src/pages/RoomListPage.tsx`
  - `onRefreshRooms`를 받아서 `useGameLogic`에서 넘겨준 함수 호출

