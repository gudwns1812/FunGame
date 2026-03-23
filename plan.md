# 행맨 게임 시작 시 로딩창 멈춤 현상 해결 계획

## 문제 원인 분석
- **초기 상태 수신 부재**: 행맨 게임 시작 시 백엔드에서 `GameStartEvent`만 보내고, 초기 단어 상태(`_ _ _ _`), 남은 기회, 첫 번째 턴 플레이어 정보를 담은 `HangmanActionEvent`를 보내지 않음.
- **게임 화면 전환 지연**: 프론트엔드의 `useGameLogic.ts`가 `ROUND_START` 이벤트를 수신해야 `status`를 `PLAYING`으로 변경하고 페이지를 전환하는데, 행맨은 단판 게임으로 설계되어 이 이벤트가 누락됨.
- **프론트엔드 렌더링 조건**: `HangmanPage.tsx`에서 `hangmanStatus`가 `null`일 경우 로딩창을 띄우고 있어, 첫 액션이 발생하기 전까지 무한 로딩에 빠짐.

## 해결 전략
1. **백엔드 수정 (`HangmanGameService.java`)**:
   - `startGame` 메서드에서 `GameStartEvent` 발행 직후, 초기 라운드 시작 알림(`RoundStartEvent`)과 초기 행맨 상태(`HangmanActionEvent`)를 발행하도록 수정. (완료)
2. **프론트엔드 수정 (`useGameLogic.ts`)**:
   - `ROUND_START` 이벤트 수신 시 `gameType`이 `HANGMAN`이라면, 서버의 `event.content`가 단어 표시 정보를 포함하고 있을 것이므로 이를 활용해 `hangmanStatus`를 최소한의 데이터로 초기화하여 로딩창을 걷어냄.
   - 서버의 `HANGMAN_ACTION` 이벤트가 늦게 오더라도 화면이 즉시 전환되도록 보장.

## 세부 작업 단계
1. `backend/src/main/java/com/fungame/songquiz/domain/HangmanGameService.java` 수정: (기존 작업 유지)
2. `frontend/src/hooks/useGameLogic.ts` 수정:
   - `ROUND_START` 핸들러 내에 행맨 초기화 로직 추가.

## 검증 계획
- 단위 테스트를 통해 `startGame` 호출 시 필요한 3가지 이벤트(`GameStartEvent`, `RoundStartEvent`, `HangmanActionEvent`)가 모두 발행되는지 확인.
- 프론트엔드에서 `/hangman` 경로로 정상 진입하여 초기 단어 상태와 "누구 차례입니다" 메시지가 바로 뜨는지 확인 (사용자 피드백 기반).
