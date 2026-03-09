# 노래 정보 패널 & 정답자 표시 수정 계획

## 버그 1: "GAME_READY_EVENT" 문구가 하루종일 고착

### 원인
`renderSongPanel()` 판단 순서:
1. `roundEndInfo` 있으면 → 정답 표시
2. `gameStartInfo` 있으면 → 게임 정보 표시 (`event.message` = `"GAME_READY_EVENT"`)
3. 아무것도 없으면 → "노래 분석 중..."

문제: `ROUND_START` 이벤트 핸들러에서 `setGameStartInfo(null)`을 호출하지 않아 `gameStartInfo`가 라운드 내내 남아있음. 그 결과 매 라운드 중에도 gameStartInfo 분기(2번)로 진입해 "GAME_READY_EVENT" 문자열이 계속 표시됨.

### 수정: `useGameLogic.ts`
`ROUND_START` 핸들러에 `setGameStartInfo(null)` 추가:
```typescript
case 'ROUND_START':
  setStatus('PLAYING');
  setCurrentVideoId(event.videoURL);
  setRoundEndInfo(null);
  setGameStartInfo(null);  // ← 추가
  setLogs([]);
  ...
```

---

## 버그 2: 정답자 `"없음"` 문자열 처리

### 원인
서버가 정답자 없을 때 `null`이 아닌 `"없음"` 문자열로 전송.
현재 `Game.tsx`에서 `roundEndInfo.winner ? ...` 로 truthy 체크 → `"없음"` 은 truthy라 정답자 이름으로 표시됨.

### 수정
**`types/game.ts`**: `winner` 타입을 `string | null` → `string`으로 변경

**`Game.tsx`**: winner 조건을 `"없음"` 문자열 비교로 변경:
```tsx
{roundEndInfo.winner && roundEndInfo.winner !== '없음' ? (
  <p>{stripTag(roundEndInfo.winner)}</p>
) : (
  <p>정답자 없음</p>
)}
```

---

## 추가 변경: 라운드 진행 중 노래 정보 패널

### 수정: `useGameLogic.ts`
- `roundIndex` 상태 추가 (`useState<number>(0)`)
- `ROUND_START` 핸들러에서 `setRoundIndex(event.roundIndex)` 저장
- 반환 객체에 `roundIndex` 포함

### 수정: `Game.tsx`
- `roundIndex` prop 추가
- 라운드 진행 중 패널(3번 분기)에서 "노래 분석 중..." 문구 제거
- 대신 현재 라운드 번호만 조용히 표시:
  ```tsx
  // 텍스트 없이 roundIndex만 표시
  <p>#{roundIndex} 번째 곡</p>
  ```

| 파일 | 변경 |
|------|------|
| `types/game.ts` | `ROUND_END.winner`: `string \| null` → `string` |
| `useGameLogic.ts` | `ROUND_START` 핸들러에 `setGameStartInfo(null)` 추가, `roundIndex` 상태 추가 및 저장 |
| `Game.tsx` | winner 조건 `→ winner && winner !== '없음'` 으로 변경, 라운드 진행 중 패널에서 "노래 분석 중..." 제거하고 "#N 번째 곡" 표시, `roundIndex` prop 추가 |
