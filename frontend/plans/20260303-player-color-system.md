# 플레이어 색상 시스템 계획서

## 개요

방 입장 시 서버로부터 받는 플레이어 슬롯 번호(`data`)를 localStorage에 저장하고, 이 번호를 기반으로 채팅 및 닉네임에 고유 색상을 적용한다.

---

## 색상 매핑

슬롯 번호 0~7을 순서대로 다음 색상에 매핑:

| 번호 | 색 | HEX |
|------|-----|-----|
| 0 | 빨 (red) | `#FF4444` |
| 1 | 주 (orange) | `#FF8C00` |
| 2 | 노 (yellow) | `#FFD700` |
| 3 | 초 (green) | `#00CC66` |
| 4 | 파 (blue) | `#4488FF` |
| 5 | 남 (indigo) | `#5544DD` |
| 6 | 보 (purple) | `#AA44FF` |
| 7 | 검 (dark) | `#AAAAAA` |

> 7번(검)은 검정 배경에서 보이도록 회색 계열로 처리

---

## 서버 응답 구조

- `joinRoom` 응답: `response.data.data` → 플레이어 슬롯 번호 (number)
- `createRoom` 응답: `response.data.data` → roomId (string, 숫자 아님)
  - 방장(호스트)은 별도 슬롯 번호를 받지 않으므로 `0`번(빨)으로 고정

---

## 변경 범위

### 1. `src/utils/playerColor.ts` [NEW]

색상 유틸 함수 파일 신규 생성:

```typescript
export const PLAYER_COLORS = [
  '#FF4444', // 0: 빨
  '#FF8C00', // 1: 주
  '#FFD700', // 2: 노
  '#00CC66', // 3: 초
  '#4488FF', // 4: 파
  '#5544DD', // 5: 남
  '#AA44FF', // 6: 보
  '#AAAAAA', // 7: 검
];

/** localStorage 키 */
export const PLAYER_COLOR_INDEX_KEY = 'ums_playerColorIndex';

/** 슬롯 번호 → 색상 HEX */
export const getPlayerColor = (index: number | null): string => {
  if (index === null || index < 0 || index >= PLAYER_COLORS.length) return '#FFFFFF';
  return PLAYER_COLORS[index];
};

/** 닉네임 → 색상 (Players 배열에서 index를 찾아 반환, fallback: 흰색) */
export const getColorByName = (
  name: string,
  players: { name: string; colorIndex?: number }[]
): string => {
  const p = players.find(p => p.name === name);
  return getPlayerColor(p?.colorIndex ?? null);
};
```

---

### 2. `src/types/game.ts`

`Player` 타입에 `colorIndex` 필드 추가:

```typescript
export interface Player {
  id: string;
  name: string;
  isHost: boolean;
  score: number;
  colorIndex?: number; // 플레이어 슬롯 번호 (0~7)
}
```

---

### 3. `src/hooks/useGameLogic.ts`

#### 3-1. 상태 추가
```typescript
const [myColorIndex, setMyColorIndex] = useState<number | null>(() => {
  const saved = localStorage.getItem('ums_playerColorIndex');
  return saved !== null ? Number(saved) : null;
});
```

#### 3-2. `joinRoom` — 슬롯 번호 저장
```typescript
if (response.data.result === 'SUCCESS') {
  const slotIndex = typeof response.data.data === 'number' ? response.data.data : null;
  if (slotIndex !== null) {
    localStorage.setItem('ums_playerColorIndex', String(slotIndex));
    setMyColorIndex(slotIndex);
  }
  // ...기존 코드...
}
```

#### 3-3. `createRoom` — 방장은 슬롯 0번으로 임시 할당
방 생성 시에는 별도 숫자를 받지 않으므로(data는 roomId 문자열) 방장을 `0`번으로 설정:
```typescript
localStorage.setItem('ums_playerColorIndex', '0');
setMyColorIndex(0);
```

#### 3-4. `leaveRoom` / `returnToLobby` — localStorage 삭제
```typescript
localStorage.removeItem('ums_playerColorIndex');
setMyColorIndex(null);
```

#### 3-5. `fetchRoomUsers` — 플레이어 목록에 colorIndex 매핑
서버가 플레이어 순서(배열 인덱스)를 통해 슬롯을 관리하므로, 배열 위치를 colorIndex로 사용:
```typescript
return players.map((name, idx) => ({
  id: name,
  name,
  isHost: name === host,
  score: prevPlayer?.score ?? 0,
  colorIndex: idx, // 배열 순서 = 슬롯 번호
}));
```

#### 3-6. return에 `myColorIndex` 추가

---

### 4. 채팅 로그 컬러링

현재 채팅 로그는 `string[]` 형태로 저장되어 누가 보낸 메시지인지 구분하기 어렵다.  
`CHAT` 이벤트 핸들러에서 로그를 저장할 때 `playerName`을 포함
하므로, 로그 파싱으로 이름을 추출해 색상을 적용한다.

**채팅 로그 형식**: `"닉네임: 메시지"` (기존 `addLog(\`${event.playerName}: ${event.message}\`)`)

#### `WaitingRoom.tsx` 변경
- `players` prop을 통해 `colorIndex`를 확인
- 채팅 로그 렌더링 시 `로그.split(':')[0]`으로 이름 추출 → players에서 `colorIndex` 조회 → 이름 부분에 `style={{ color }}` 적용
- 플레이어 슬롯 카드의 닉네임 텍스트에도 색상 적용

#### `Game.tsx` 변경
- 동일한 방식으로 채팅 로그 이름 부분에 색상 적용
- 랭킹 패널의 닉네임에도 `colorIndex` 기반 색상 적용

---

## 변경 파일 요약

| 파일 | 변경 |
|------|------|
| `src/utils/playerColor.ts` | [NEW] 색상 상수 및 유틸 함수 |
| `src/types/game.ts` | `Player`에 `colorIndex?: number` 추가 |
| `src/hooks/useGameLogic.ts` | `myColorIndex` 상태, joinRoom/createRoom/leaveRoom/returnToLobby에 localStorage 저장/삭제, fetchRoomUsers에 colorIndex 매핑, return에 포함 |
| `src/components/WaitingRoom.tsx` | 슬롯 카드 닉네임 + 채팅 로그 이름 색상 적용 |
| `src/components/Game.tsx` | 랭킹 패널 닉네임 + 채팅 로그 이름 색상 적용 |

---

## 검증 방법

### 수동 테스트
1. `npm run dev` 실행 후 브라우저에서 두 탭(플레이어 A, B) 열기
2. A가 방 생성 → localStorage `ums_playerColorIndex` = `0` 확인 (DevTools → Application → Local Storage)
3. B가 방 입장 → localStorage `ums_playerColorIndex` = `1` (또는 서버 응답 값) 확인
4. 대기실에서 A, B의 슬롯 카드 닉네임 색상이 다른지 확인
5. 채팅 메시지 전송 시 이름 부분 색상 확인
6. 게임 진행 중 랭킹 패널 닉네임 색상 확인
7. 로비로 나가기 후 localStorage에서 `ums_playerColorIndex` 삭제 확인
