---
name: backend-layered-architecture-skill
description: domain(비즈니스)과 storage(영속성) 계층을 분리하고 의존성 규칙을 준수하는 아키텍처 설계 지침
---

# Backend Layered Architecture Skill

이 스킬은 `FunGame` 프로젝트의 핵심 아키텍처인 계층 분리 원칙을 준수하기 위한 가이드입니다. 비즈니스 로직의 순수성을 보장하고 인프라(DB)와의 결합도를 낮추는 것을 목적으로 합니다.

## 핵심 원칙

### 1. 계층 구조 및 의존성 방향
- **Controller (Web)** -> **Service (Orchestration)** -> **Domain (Business Logic)**
- **Service (Orchestration)** -> **Storage (Persistence)**
- **중요:** `Domain` 계층은 `Storage`(Entity, Repository)를 절대 의존하지 않으며, 순수 자바 객체로만 구성됩니다.

### 2. 패키지 역할 정의
- **`com.fungame.songquiz.domain`**: 
    - 비즈니스 핵심 규칙과 상태를 가짐.
    - JPA 애노테이션을 사용하지 않는 순수 POJO 엔티티.
- **`com.fungame.songquiz.storage`**:
    - DB 테이블 구조와 매핑되는 `@Entity` 클래스.
    - `JpaRepository`를 상속받는 인터페이스.
    - 도메인 객체로의 변환을 담당 (`toDomain()` 메서드).

## 구현 가이드라인

### Entity to Domain 변환
`Storage` 계층의 엔티티는 도메인 객체로 변환하는 메서드를 반드시 포함해야 합니다.

```java
@Entity
public class SongEntity {
    // ... 필드 정의
    
    public Song toDomain() {
        return Song.of(title, singer, categories, releaseDate, videoLink, playSeconds, answers, hint);
    }
}
```

### Service의 역할
서비스 계층은 `Repository`에서 엔티티를 조회한 뒤, `toDomain()`을 통해 비즈니스 로직을 수행하고 다시 저장하는 '오케스트레이터' 역할을 수행합니다.

```java
@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;

    public Song getSong(Long id) {
        return songRepository.findById(id)
                .map(SongEntity::toDomain) // 변환 후 도메인 객체 반환
                .orElseThrow(() -> new CoreException(ErrorType.SONG_NOT_FOUND));
    }
}
```
