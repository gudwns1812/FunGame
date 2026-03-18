---
name: ddd-entity-design-skill
description: 무분별한 Setter를 지양하고, 비즈니스 로직을 객체 내부에 응집시키는 도메인 객체 설계 지침
---

# DDD Entity Design Skill

이 스킬은 도메인 주도 설계(DDD)와 객체 지향 프로그래밍(OOP) 원칙에 따라 엔티티(Entity) 및 도메인 모델을 작성하거나 리팩토링할 때 적용하는 워크플로우입니다.

## Workflow

### 1. 객체 생성 및 초기화 (Creation)

- **무분별한 Setter 금지:** 클래스 레벨에 `@Setter`를 절대 사용하지 않습니다.
- **의도 지향적 생성:** 객체의 생성은 정적 팩토리 메서드(`of()`, `create()`)를 통해서만 이루어집니다.
    - 예: `public static GameRoom create(...)`, `public static Song of(...)`
- **유효성 검사:** 객체가 생성되는 시점에 상태가 유효하지 않을 경우 반드시 `CoreException`을 발생시킵니다.

### 2. 상태 변경 및 행위 (Behaviors)

- 필드 값을 변경할 때는 비즈니스 의미가 담긴 메서드명(예: `join()`, `start()`, `isCorrect()`)을 사용합니다.
- 상태 변경 시 해당 도메인 규칙에 어긋나지 않는지 객체 내부의 `validateXxx()` 메서드에서 항상 검증합니다.

### 3. 로직의 분산과 구현 계층 (Implementation Layer)

서비스 계층이 복잡해지는 것을 방지하고 비즈니스 흐름을 명확히 하기 위해 **Implementation(Reader/Writer) 계층**을 적극 활용합니다.

#### 도메인 모델 (Entity/VO)
- **책임**: 상태 관리 및 자가 검증.
- **특징**: 외부 의존성(Spring, Repository 등)이 없는 순수 객체.

#### 구현 계층 (Reader/Writer/Implementation)
- **책임**: 특정 도메인의 복잡한 데이터 조회, 가공, 변환(Entity -> Domain) 전담.
- **특징**: `@Component`로 선언하며, 특정 기술적 구현(JPA 호출, JSON 파싱 등)을 캡슐화하여 서비스의 복잡도를 흡수.
- **예시**: `SongReader`, `GameSessionManager`

#### 서비스 계층 (Service)
- **책임**: 비즈니스 시나리오의 전체적인 흐름 제어 (Orchestration).
- **특징**: 세부 구현은 `Reader/Writer`에게 위임하고, 도메인 모델의 메서드를 호출하여 비즈니스 목적을 달성하는 '고수준 흐름'에 집중.

---

### 로직 배치 원칙
1. **자가 상태 검증/변경**: 도메인 객체 내부.
2. **복잡한 데이터 조회/기술적 가공**: `Reader/Writer` (Implementation).
3. **여러 도메인의 협력/전체 흐름 제어**: `Service`.
