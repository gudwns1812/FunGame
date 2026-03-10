# 할리갈리(HaliGali) 게임 백엔드 구현 작업 목록 (tasks.md)

본 문서는 `GEMINI.md`의 TDD 및 작업 절차 가이드라인과 `backend/BACKEND.md`의 아키텍처 명세를 바탕으로 `plan.md`의 할리갈리 게임 구현을 달성하기 위해 작성된 단계별 작업 지시서입니다.

## Phase 1: HaliGaliGame 도메인 모델 및 핵심 로직 (TDD)
- [x] `backend/src/test/java/com/fungame/songquiz/domain/HaliGaliGameTest.java` 작성
    - [x] 56장의 카드 덱 생성 및 셔플 검증 테스트
    - [x] 플레이어별 카드 분배 로직 테스트
    - [x] `FLIP_CARD` 액션 시 카드 이동 및 턴 전환 테스트
    - [x] `RING_BELL` 액션 시 바닥 카드 합계(5) 판정 및 카드 획득 테스트
    - [x] 잘못된 종 울리기(Penalty) 시 카드 배분 테스트
    - [x] 카드 소진 시 플레이어 탈락 및 최종 승리자 판정 테스트
- [x] `backend/src/main/java/com/fungame/songquiz/domain/HaliGaliGame.java` 구현
    - [x] 과일 타입(Fruit) 및 카드(Card) 내부 클래스/레코드 정의
    - [x] `Game` 인터페이스 메서드 구현 (`handleAction`, `getStatus` 등)
    - [x] 액션 처리 로직 (`FLIP_CARD`, `RING_BELL`) 완성

## Phase 2: BoardGameService 기능 완성 및 통합
- [x] `backend/src/main/java/com/fungame/songquiz/domain/BoardGameService.java` 구현
    - [x] `GameService` 인터페이스 메서드 구현
    - [x] `GameSessionManager`를 통한 할리갈리 세션 생성 및 관리 로직 연동
    - [x] `handleAction` 결과를 바탕으로 게임 이벤트(HaliGaliEvent) 발행 로직 추가
- [x] `GameServiceRouter`에 `BoardGameService`가 정상적으로 등록되는지 확인

## Phase 3: WebSocket 메시지 정의 및 API 문서화 (RestDocs)
- [x] 할리갈리 전용 WebSocket 응답 DTO 정의 (`HaliGaliActionEvent` 활용)
- [ ] `backend/src/test/java/com/fungame/songquiz/controller/api/HaliGaliApiControllerTest.java` 작성
    - [ ] `@AutoConfigureRestDocs`를 활용한 API 명세 생성
    - [ ] `api/halligali.md` 문서 자동 생성 확인

## Phase 4: 최종 검증 및 마무리
- [ ] 전체 백엔드 테스트(`./gradlew test`) 실행 및 통과 확인
- [ ] 인메모리 H2 DB 환경에서 할리갈리 게임 세션 생성 및 액션 처리 정상 동작 확인