# UX 개선: 페이지 Flash 없이 즉시 이동

## 문제

현재 흐름:
```
앱 마운트 → localStorage 상태 복원 → 페이지 렌더링 (Flash) → API 검증 실패 → 다른 페이지로 이동
```

`PLAYING`/`WAITING` 상태로 새로고침 시:
1. localStorage의 status를 그대로 읽어 게임/대기 페이지를 **먼저 렌더링**함
2. 마운트 후 `POST /join` 또는 `fetchRank` API 호출
3. 실패 시 `ROOM_LIST`로 이동

이 과정에서 잘못된 페이지가 **순간 노출**됨.

---

## 해결 방법: Bootstrap 검증 상태 도입

### 핵심 아이디어
앱 최초 마운트 시 **검증이 완료될 때까지 아무 페이지도 렌더링하지 않고** 로딩 스크린을 보여줌.
검증 완료 후 올바른 페이지로 바로 이동.

```
앱 마운트 → 로딩 스크린 → API 검증 → 올바른 페이지 렌더링 (단 1번)
```

---

## 변경 파일

### `useGameLogic.ts`
- `isBootstrapping: boolean` 상태 추가 (`useState(true)`)
- 마운트 시 실행되는 `useEffect`(현재 `[]` deps)를 **async** 로직으로 재작성:
  - `PLAYING` + `roomId` 있음 → `await axios.post(.../join)`:
    - 성공 → WebSocket 연결 → `setIsBootstrapping(false)`
    - 실패 → 상태 초기화 → `setIsBootstrapping(false)`
  - `WAITING` + `roomId` 있음 → WebSocket 연결 → `setIsBootstrapping(false)`
  - 그 외 → `setIsBootstrapping(false)` 즉시
- 반환 객체에 `isBootstrapping` 포함

### `App.tsx`
- `isBootstrapping` 구조분해
- `isBootstrapping === true`이면 라우터 대신 **로딩 스크린** 렌더링:
  ```tsx
  if (isBootstrapping) return <BootstrapScreen />;
  ```
- `BootstrapScreen`: 풀스크린 중앙에 스피너 또는 로딩 텍스트

### fetchRank 에러 처리 (추가)
- `Game.tsx` 마운트 시 `onFetchRank()` 실패해도 **ROOM_LIST로 이동하지 않음**
- `isBootstrapping` 완료 후 이미 상태가 검증됐으므로 랭킹 실패는 치명적이지 않음
- `fetchRank` catch 블록에서 `setStatus('ROOM_LIST')` 제거

---

## 변경 파일 요약

| 파일 | 변경 |
|------|------|
| `useGameLogic.ts` | `isBootstrapping` 상태 추가, 마운트 effect async 재작성 |
| `App.tsx` | `isBootstrapping` 중 로딩 스크린 표시 |
| `useGameLogic.ts` | `fetchRank` catch에서 ROOM_LIST redirect 제거 |
