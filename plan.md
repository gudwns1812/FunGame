# 백엔드 게임 서비스 구조 리팩토링 계획

## 1. 배경 및 목표
현재 `GameService`는 퀴즈(시간/정답 기반) 로직에 강하게 결합되어 있어, 할리갈리와 같은 액션 기반 게임을 추가하기 어렵습니다. 이를 해결하기 위해 `GameService`를 인터페이스로 추상화하고, 게임 타입별로 독립적인 엔진(구현체)을 가질 수 있는 전략 패턴 구조로 리팩토링합니다.

## 2. 핵심 변경 전략
- **추상화**: `GameService`를 인터페이스로 전환하여 공통 명세를 정의합니다.
- **분리**: 기존 퀴즈 로직을 `QuizGameService`로 이동시켜 응집도를 높입니다.
- **유연성**: `GameType`에 따라 적절한 서비스 구현체를 선택하는 라우팅 구조를 도입합니다.
- **안정성**: 기존에 작성된 통합 테스트(`GameServiceIntegrationTest`)를 통해 리팩토링 후에도 기존 기능이 유지됨을 보장합니다.

## 3. 상세 작업 단계

### Phase 1: GameService 인터페이스 정의 및 QuizGameService 분리
- **인터페이스 추출**: `GameService` 인터페이스 생성 및 주요 메서드(`startGame`, `processAnswer`, `increaseSkipVote`, `getPlayerRanks`) 정의.
- **구현체 이동**: 기존 `GameService` 클래스를 `QuizGameService`로 변경하고 인터페이스를 구현하도록 수정.
- **테스트 검증**: `GameServiceIntegrationTest`를 실행하여 퀴즈 게임 흐름 확인.

### Phase 2: 게임 타입별 전략 라우팅 구조 구축
- **GameServiceRouter 구현**: `GameType`에 따라 `QuizGameService` 또는 향후 추가될 `ActionGameService`로 요청을 위임하는 라우터 클래스 구현.
- **의존성 주입 최적화**: 컨트롤러와 이벤트 리스너가 인터페이스(`GameService`)를 바라보도록 설정하고, 실제 빈 주입은 라우터를 통하도록 구성.

### Phase 3: 공통 로직 및 예외 처리 고도화
- **공통 기능 추출**: `GameSession` 조회, 사용자 검증 등 여러 게임에서 공통으로 쓰이는 로직을 별도 컴포넌트나 추상 클래스로 분리.
- **액션 인터페이스 확장**: `processAnswer` 메서드를 다양한 액션을 수용할 수 있는 형태로 일반화 고민.

## 4. 검증 계획
1. **회귀 테스트**: `GameServiceIntegrationTest`가 리팩토링 후에도 100% 통과하는지 확인.
2. **의존성 체크**: 프로젝트 전체 빌드 및 컴파일 에러 여부 확인.
3. **확장성 테스트**: (선택 사항) 간단한 Mock 서비스(`HalliGalliGameService`)를 추가하여 라우팅이 정상적으로 동작하는지 확인.
