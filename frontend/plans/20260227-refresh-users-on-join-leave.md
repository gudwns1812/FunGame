# Join/Leave 이벤트 수신 시 유저 목록 강제 리프레시

## 배경 / 문제
- 현재는 `PLAYER_JOIN`, `PLAYER_LEAVE` 이벤트를 받으면 클라이언트에서 `players` 상태를 “추가/삭제/치환” 방식으로만 갱신함.
- 이벤트가 누락되거나(특히 새로 들어온 사람의 구독 타이밍), 서버/클라이언트 상태가 어긋나면 유저 목록이 틀어질 수 있음.

## 목표
- 앞으로 `PLAYER_JOIN`, `PLAYER_LEAVE` 이벤트를 받으면 **이벤트 payload만 믿지 않고**,
  `GET /game/rooms/{roomId}/users`를 호출해 **서버 기준으로 유저 목록을 즉시 동기화**한다.

## 현 상태(관련 코드)
- 훅: `src/hooks/useGameLogic.ts`
  - 이미 `fetchRoomUsers(roomId)`가 존재하고, `WAITING` 진입/새로고침 시 1회 동기화도 수행 중.
  - 웹소켓 이벤트 처리: `handleEvent(event)`

## 변경 계획
### 1) `PLAYER_JOIN`, `PLAYER_LEAVE` 처리 로직 변경
- 변경 전:
  - `event.players` 또는 `event.nickname`을 이용해 `setPlayers(...)`로 직접 업데이트
- 변경 후:
  - `PLAYER_JOIN` 또는 `PLAYER_LEAVE` 이벤트 수신 시:
    - `roomId`가 존재하면 `fetchRoomUsers(roomId)` 호출
    - `addLog`는 “동기화 중/완료” 정도로만 남김(원하면 로그 문구 조정)
  - 이로써 서버 이벤트 누락/순서 문제에도 항상 서버 기준으로 “리프레시”

### 2) (선택) 과도한 GET 호출 방지(디바운스/쿨다운)
이벤트가 짧은 시간에 여러 번 오면 `/users`를 너무 자주 칠 수 있음.
- 옵션 A: 간단 쿨다운(예: 300~500ms 안에 여러 번 오면 1번만 호출)
- 옵션 B: 마지막 요청이 진행 중이면 중복 호출 스킵, 완료 후 1회만 추가 요청

> 기본 구현은 **옵션 A 없이**도 동작은 하지만, 트래픽/성능 고려하면 A 또는 B 권장.

### 3) 타입(`GameEvent`) 처리
서버가 `PLAYER_JOIN/LEAVE`에 어떤 payload를 실어 보내든, 클라이언트는 “트리거”로만 사용.
- 유지안: `PLAYER_JOIN/LEAVE` 타입을 그대로 두고, 내부에서 payload는 사용하지 않음
- 정리안: `PLAYER_JOIN/LEAVE`를 `{ type: 'PLAYER_JOIN' }`처럼 최소화(서버와 계약 맞춰야 하므로 보류 가능)

## 테스트 시나리오
- A가 방 생성 → B가 입장:
  - B 화면에서 `/waiting` 진입 시 `/users`로 전체 목록 표시
  - A/B 둘 다에서 JOIN 이벤트 이후 `/users` 호출로 목록 동일해야 함
- B가 뒤로가기/나가기:
  - LEAVE 이벤트 수신 후 `/users` 호출로 목록에서 B 제거 확인
- 새로고침:
  - `/waiting` 새로고침 시 `/users`로 목록 복구 확인

## 리스크 / 주의사항
- `/users` API가 늦거나 실패할 때:
  - UI가 잠깐 옛 목록을 보여줄 수 있음 → 실패 시 재시도/토스트 등은 추후 옵션
- 닉네임 헤더(한글):
  - HTTP 헤더는 비-ASCII 불가 → REST 요청 헤더의 `nickname`은 `encodeURIComponent` 유지 필요

## 작업 범위(수정 파일)
- (필수) `src/hooks/useGameLogic.ts`
- (선택) `src/types/game.ts` (타입 정리할 경우)

