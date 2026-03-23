# 행맨(Hangman) 게임 난이도(diff 1-5) API 연동 Todo 리스트

## 1단계: 백엔드 단어 공급 시스템 고도화 (담당: backend-dev)
- [ ] `HangmanWordProvider` 인터페이스 수정: `getWord(int difficulty)` 추가
- [ ] `HangmanRandomWordApiProvider` 구현 (신규)
    - [ ] `random-word-api` 호출 로직 구현
        - [ ] URL: `https://random-word-api.herokuapp.com/word?number=1&diff={difficulty}`
        - [ ] `difficulty`는 사용자로부터 전달받은 1~5 사이의 값 적용
    - [ ] API 응답(JSON Array) 파싱 및 단어 추출 로직 구현
- [ ] `HangmanWordReader` 수정: 사용자가 선택한 난이도를 공급자에게 전달하도록 변경

## 2단계: 게임 생성 및 시작 로직 수정 (담당: backend-dev, backend-security)
- [ ] `HangmanGameService.startGame` 수정: 
    - [ ] 클라이언트로부터 난이도(1-5) 수신
    - [ ] `ErrorType.INVALID_INPUT_VALUE`를 이용한 난이도 범위(1-5) 검증 추가
- [ ] 행맨 게임 시작 시 필요한 DTO 업데이트 (난이도 정보 포함)

## 3단계: 프론트엔드 UI/UX 확장 (담당: frontend-builder)
- [ ] 대기실/방 생성 UI 수정
    - [ ] 행맨 게임 선택 시 **난이도 선택 UI (1~5 단계)** 추가
    - [ ] 선택된 난이도 값을 로컬 상태로 관리
- [ ] 게임 시작 요청 시 서버로 선택된 난이도(`diff`)를 파라미터로 전송

## 4단계: 테스트 및 검증 (담당: backend-tdd, frontend-verifier)
- [ ] **API 연동 단위 테스트**: `diff` 파라미터가 1~5일 때 각각 올바른 URL로 요청이 가는지 검증 (MockRestServiceServer 활용)
- [ ] 프론트엔드 난이도 선택 UI 컴포넌트 테스트
- [ ] 전체 게임 루프 최종 확인
