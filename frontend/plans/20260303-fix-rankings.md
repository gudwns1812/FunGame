# 결과 화면 rankings 파싱 & 라운드별 랭킹 업데이트 계획

## 버그 1: GAME_RESULT rankings 파싱 오류

### 현재 상황
서버가 `rankings`를 `Record<string, number>` (key-value 객체)가 아닌 아래 형식으로 전송:
```
"playerName:score\nplayerName2:score2\n..."
```

### 현재 코드 (L158-164, useGameLogic.ts)
```typescript
const finalRankings: Player[] = Object.entries(event.rankings).map(...)
```
→ `Object.entries`가 문자열에 적용되어 올바르게 파싱 안 됨

### 수정: `types/game.ts`
```typescript
// 변경 전
| { type: 'GAME_RESULT'; rankings: Record<string, number> }
// 변경 후
| { type: 'GAME_RESULT'; rankings: string }
```

### 수정: `useGameLogic.ts` — GAME_RESULT 핸들러
```typescript
case 'GAME_RESULT': {
  // "playerName:score\nplayerName2:score2" 형식 파싱
  const finalRankings: Player[] = event.rankings
    .split('\n')
    .filter(line => line.trim() !== '')
    .map(line => {
      const colonIdx = line.lastIndexOf(':');
      const name = line.substring(0, colonIdx).trim();
      const score = parseInt(line.substring(colonIdx + 1).trim(), 10) || 0;
      return { id: name, name, score, isHost: false };
    });
  setPlayers(finalRankings);
  ...
}
```

---

## 기능 추가: ROUND_END 시 랭킹 업데이트

### 현재 상황
- `ROUND_END` 이벤트가 왔을 때 랭킹을 업데이트하지 않음
- `fetchRank()`는 있지만 ROUND_END에서 호출하지 않음

### 수정: `useGameLogic.ts` — ROUND_END 핸들러에 fetchRank 호출 추가
```typescript
case 'ROUND_END':
  setRoundEndInfo({ answer: event.answer, winner: event.winner });
  fetchRank(); // 라운드 종료 시 최신 랭킹 갱신
  ...
  break;
```

> `fetchRank`가 이미 `useCallback`으로 정의되어 있으므로 `handleEvent`의 deps에 추가 필요

---

## 변경 파일 요약

| 파일 | 변경 |
|------|------|
| `types/game.ts` | `GAME_RESULT.rankings`: `Record<string, number>` → `string` |
| `useGameLogic.ts` | GAME_RESULT 핸들러 파싱 로직 변경, ROUND_END에서 `fetchRank()` 호출, `handleEvent` deps에 `fetchRank` 추가 |
