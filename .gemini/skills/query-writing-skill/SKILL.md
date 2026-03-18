---
name: query-writing-skill
description: Spring Data JPA와 QueryDSL을 활용하여 효율적이고 안전한 데이터베이스 쿼리를 작성하는 지침
---

# Query Writing Skill

이 스킬은 데이터베이스에 접근하여 데이터를 다룰 때, 성능과 코드의 안전성(Type-safety)을 보장하기 위한 쿼리 작성 표준 워크플로우입니다.

## Workflow

### 1. 단순 CRUD 및 기본 조회 (Spring Data JPA)

- 단일 엔티티의 저장, 단건 조회, 삭제 등 단순한 CRUD 작업은 **Spring Data JPA**가 제공하는 기본 메서드(`save()`, `findById()`, `delete()`)를 사용합니다.
- 검색 조건이 1~2개로 고정되어 있는 단순한 정적 쿼리는 Spring Data JPA의 쿼리 메서드(`findByXxxAndYyy`) 기능을 활용하여 간결하게 작성합니다.

### 2. 복잡한 쿼리 및 동적 쿼리 (QueryDSL)

- 다중 조인(Join)이 필요하거나, 사용자의 입력에 따라 조건이 달라지는 동적 쿼리, 혹은 복잡한 DTO 투영(Projection)이 필요한 경우에는 `@Query`를 이용한 JPQL 문자열 작성이나 **Native Query 사용을 엄격히 금지**합니다.
- 복잡한 쿼리는 반드시 **QueryDSL**(`JPAQueryFactory`)을 사용하여 자바 코드로 작성합니다. 이를 통해 컴파일 타임에 문법 오류를 잡고 자동완성 기능을 적극 활용합니다.

### 3. 조건절 분리 및 가독성 확보 (Clean Code)

- QueryDSL로 동적 쿼리를 작성할 때, `Where` 절 안에 들어가는 조건들은 `BooleanExpression`을 반환하는 별도의 작은 메서드로 분리합니다.
- (예: `isPublished()`, `containsTitle()`) 이를 통해 쿼리의 의도를 명확히 하고 조건 로직을 재사용할 수 있게 만듭니다.

### 4. 성능 및 N+1 문제 방지

- 연관된 엔티티를 함께 조회해야 하는 상황에서는 지연 로딩(Lazy Loading)으로 인한 N+1 문제가 발생하지 않도록 QueryDSL의 `fetchJoin()`을 명시적으로 사용합니다.
