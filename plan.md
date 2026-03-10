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

## 5. [긴급] application.yml Git 추적 해제 작업

### 배경
- `backend/src/main/resources/application.yml` 파일이 현재 Git에 포함되어 GitHub에 공유되고 있습니다.
- 이 파일에는 민감한 설정 정보가 포함될 수 있으므로, Git 추적을 중단하고 로컬에서만 관리하도록 설정해야 합니다.

### 작업 단계
1.  **Git 인덱스에서 제거**: `git rm --cached` 명령을 사용하여 로컬 파일은 유지하면서 Git 추적만 중단합니다.
2.  **.gitignore 확인**: `backend/.gitignore`에 `application.yml`이 이미 포함되어 있는지 다시 확인합니다. (확인 결과: 이미 포함됨)
3.  **변경사항 커밋**: 추적 해제된 상태를 커밋하여 원격 저장소에서도 해당 파일이 삭제되도록 합니다. (주의: 다른 개발자가 pull 할 경우 해당 파일이 로컬에서 삭제될 수 있으므로 사전에 공지가 필요합니다.)

### 검증 계획
1.  `git ls-files` 명령을 통해 해당 파일이 더 이상 추적되지 않는지 확인합니다.
2.  로컬 파일 시스템에 `backend/src/main/resources/application.yml`이 여전히 존재하는지 확인합니다.
