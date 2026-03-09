# 프론트엔드 UX 개선 및 시스템 최적화 작업 목록 (tasks.md)

본 문서는 `GEMINI.md`의 TDD 및 작업 절차 가이드라인과 `frontend/guideLine.md`의 아키텍처 명세를 바탕으로 `plan.md`를 달성하기 위해 작성된 단계별 작업 지시서입니다.

## Phase 1: 테스트 환경 구축 및 TDD용 테스트 코드 작성
- [ ] `frontend/src/hooks/useGameLogic.test.ts`: 이벤트 발생(JOIN, LEAVE, 그 외 이벤트)에 따른 로그 출력 필터링 검증 테스트 작성
- [ ] `frontend/src/components/WaitingRoom.test.tsx`: 전역 Enter 키 입력 이벤트 발생 시 채팅 입력창 포커스 이동 검증 테스트 작성
- [ ] `frontend/src/components/Game.test.tsx`: CS 퀴즈 진입 시 패널 고정 높이, 폰트 축소 클래스 적용 및 채팅 포커스/스크롤 렌더링 검증 테스트 작성

## Phase 2: 상태 관리 로직 (`useGameLogic.ts`) 시스템 최적화
- [ ] `PLAYER_JOIN`과 `PLAYER_LEAVE` 이벤트 이외의 불필요한 이벤트 로그(준비 완료, 정답자 발생, 라운드 종료 등) 출력 제거
- [ ] `ROUND_START` 등의 이벤트 시 출력되는 라운드 구분선(`------------------------------------------------------------`) 확장 적용

## Phase 3: 키보드 인터랙션 및 채팅 레이아웃 개선
- [ ] `WaitingRoom.tsx`에 `전역 keydown (Enter)` 감지 로직 적용하여 즉시 채팅 입력창으로 포커스 이동 구현
- [ ] `Game.tsx`에도 동일하게 Enter 키 포커스 자동 이동 로직 적용 (단, 이미 포커싱된 상태면 메시지 전송 처리 유지)
- [ ] 채팅 메시지 목록 컨테이너에 고정 높이(`h-[500px]` 또는 `flex-1`), `overflow-y-auto` 및 커스텀 스크롤 속성 적용하여 화면 흔들림 방지
- [ ] 새로운 채팅 메시지가 추가될 때마다 자동으로 스크롤을 맨 아래로 이동시켜 항상 최신 채팅이 보이도록 구현 (`useRef` 및 `scrollIntoView` 활용)

## Phase 4: CS 퀴즈 게임 UI 렌더링 최적화
- [ ] `Game.tsx` 내부 CS 퀴즈 질문 컴포넌트의 폰트 사이즈 조정 (텍스트가 길어도 한눈에 파악 가능하도록)
- [ ] 중앙 퀴즈 패널 컨테이너의 `min-height`를 넉넉하게 고정하여 질문 길이가 길고 짧음에 관계없이 UI 레이아웃이 요동치지 않게 안정화

## Phase 5: 최종 검증 및 마무리 절차
- [ ] 작성된 모든 TDD 기반 유닛/통합 테스트 스위트 통과 확인
- [ ] `plan.md`의 "검증 계획" 4가지(채팅 기능, 레이아웃, 로그, UI) 브라우저 렌더링 검증 파악
- [ ] 사용자의 변경사항 확인 및 커밋(commit) / 푸시(push) 허가 대기 (GEMINI.md 가이드라인)
