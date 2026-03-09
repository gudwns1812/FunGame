# 백엔드 개발 가이드라인 (GUIDELINE.md)

본 문서는 `FunGame` 백엔드 시스템의 아키텍처, 패키지 구조, 설계 원칙 및 코딩 표준을 정의합니다. 모든 개발자는 본 가이드를 준수하여 프로젝트의 일관성을 유지해야 합니다.

---

### 1. 아키텍처 개요 (Architecture Overview)

본 프로젝트는 **3계층 레이어드 아키텍처(3-Tier Layered Architecture)**를 기본으로 하며, **도메인 중심 설계(Domain-Driven Design)**와 **이벤트 기반 아키텍처(Event-Driven Architecture)** 요소를 결합하여 설계되었습니다.

- **Presentation Layer (Web/API)**: 클라이언트의 요청(HTTP/WebSocket)을 처리하고 응답을 반환합니다.
- **Domain Layer (Business Logic)**: 핵심 비즈니스 로직과 도메인 모델의 상태를 관리합니다.
- **Infrastructure Layer (Persistence/Storage)**: 데이터의 영속성(DB) 및 외부 시스템과의 연동을 담당합니다.

---

### 2. 패키지 구조 및 역할 (Package Structure)

모든 소스 코드는 `com.fungame.songquiz` 하위의 다음 패키지 구조를 따릅니다.

#### A. `controller` (Presentation Layer)
- **역할**: 외부 요청의 진입점입니다.
- **하위 구조**:
    - `api`: RESTful API 컨트롤러.
    - `websocket`: WebSocket(STOMP) 핸들러 및 메시지 처리.
    - `config`: Web, WebSocket, Argument Resolver 등의 설정.
    - `request/response`: 요청 및 응답에 최적화된 DTO.

#### B. `domain` (Domain Layer)
- **역할**: 시스템의 핵심 가치와 비즈니스 로직이 응집된 곳입니다.
- **하위 구조**:
    - `Entity/Model`: `Game`, `GameRoom`, `Song` 등 행위와 상태를 가진 도메인 객체.
    - `Service`: 여러 도메인 객체의 협력을 조율하거나 영속성 레이어와의 연결을 담당.
    - `event`: 시스템 내부 상태 변화를 알리는 이벤트 객체 및 이벤트 발행 로직.
    - `gamecreator`: 다양한 게임 모드를 생성하기 위한 팩토리 클래스들.

#### C. `storage` (Infrastructure Layer)
- **역할**: 데이터베이스와의 상호작용을 담당합니다.
- **구성**: JPA `Entity`, `Repository` 인터페이스 및 데이터 변환 유틸리티(`converter`).

#### D. `support` (Common Utilities)
- **역할**: 전역적으로 사용되는 공통 기능을 제공합니다.
- **구성**: 공통 응답 포맷(`ApiResponse`), 예외 처리(`CoreException`, `ErrorCode`).

---

### 3. 주요 설계 스타일 (Design Styles)

#### A. 도메인 중심 설계 (DDD)
- 서비스 클래스에 비즈니스 로직을 몰아넣는 '빈약한 도메인 모델' 대신, **도메인 객체가 스스로의 상태를 변경하고 비즈니스 규칙을 검증**하는 '풍부한 도메인 모델'을 지향합니다.
- 예: `GameRoom.joinPlayer()`, `Game.processAnswer()`

#### B. 이벤트 기반 상태 전파
- 도메인 모델의 상태가 변하면 `ApplicationEventPublisher`를 통해 이벤트를 발행합니다.
- 이를 통해 비즈니스 로직과 알림 로직(WebSocket 전송 등)을 분리하여 결합도를 낮춥니다.
- 예: 게임 시작 시 `GameStartEvent` 발행 -> `GameNotifyService`에서 WebSocket 메시지 전송.

#### C. 확장 가능한 게임 생성 (Factory Pattern)
- `GameFactory` 인터페이스를 통해 다양한 게임 타입(노래 퀴즈, CS 퀴즈 등)을 일관된 방식으로 생성합니다.
- 새로운 게임 모드 추가 시 기존 코드를 최소한으로 수정하며 확장할 수 있습니다.

---

### 4. 코딩 표준 및 규칙 (Coding Standards)

#### A. 명명 규칙 (Naming)
- 클래스: `PascalCase` (명사형)
- 메서드/변수: `camelCase` (동사/명사형)
- 상수: `UPPER_SNAKE_CASE`
- 테스트 메서드: `test_기능설명()` 또는 한글 명명 허용.

#### B. 예외 처리 (Exception Handling)
- 체크 예외 대신 **런타임 예외(`CoreException`)** 사용을 원칙으로 합니다.
- 모든 비즈니스 에러는 `ErrorCode`에 정의하며, `ApiControllerAdvice`에서 공통 응답 포맷으로 변환되어 클라이언트에 전달됩니다.

#### C. 코드 스타일
- **Lombok 활용**: `@Getter`, `@RequiredArgsConstructor` 등을 적극 활용하되, `@Data`나 `@AllArgsConstructor`는 지양합니다.
- **생성자 주입**: 의존성 주입은 항상 생성자 주입 방식을 사용합니다.

---

### 5. 테스트 전략 (Testing Strategy)

- **TDD (Test-Driven Development)**: 핵심 도메인 로직은 반드시 테스트 코드를 먼저 작성하여 검증합니다.
- **단위 테스트**: `domain` 로직은 외부 의존성 없이 JUnit5와 AssertJ를 사용하여 빠르게 검증합니다.
- **인수 테스트 (Acceptance Test)**: `src/test/.../acceptance` 패키지에서 전체 API 흐름이 정상 작동하는지 `RestAssured` 등을 사용하여 검증합니다.

---
본 가이드라인은 프로젝트의 발전과 함께 지속적으로 업데이트됩니다.
