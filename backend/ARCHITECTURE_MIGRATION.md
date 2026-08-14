# ARCHITECTURE MIGRATION

[ARCHITECTURE.md](./ARCHITECTURE.md)의 목표 구조로 가는 단계별 계획.

## 전제

**논리 구조(4계층)를 먼저 정리하고, 물리 구조(gradle 모듈)를 마지막에 쪼갠다.**

모듈부터 쪼개면 첫 PR에서 컴파일이 깨진다. 지금 `storage`가 `domain`을 거꾸로 의존하는 지점이 있기 때문이다.

```
storage/GameRoomStore.java      -> domain.GameRoom, domain.RoomSettings, domain.member.MemberRepository
storage/GameRoomEntity.java     -> domain.GameType, domain.Category, domain.RoomSettings, domain.GamePlayer, ...
storage/SongEntity.java         -> domain.Song, domain.Category
storage/ComputerScienceEntity.java -> domain.ComputerScienceQuiz, domain.CSQuizDifficulty
storage/ComputerScienceRepository.java -> domain.CSQuizDifficulty
```

`storage:db-core`는 `core:core-enum`만 의존해야 하므로 이 5개 파일이 그대로면 모듈 분리가 불가능하다. 반대로 계층을 먼저 정리해두면 마지막 모듈 분리는 **파일 이동 + build.gradle 작성**만 남는 기계적인 작업이 된다.

## 규칙의 적용 범위

4계층 의존 규칙은 **협력 객체(스프링 빈)** 에 적용한다.

| 대상 | 규칙 적용 |
| --- | --- |
| Controller, Service, Reader/Writer 등 구현 객체, Repository | 적용 |
| 도메인 모델 (`GameRoom`, `HangmanGame`, `Member`, `Song`) | 미적용 |
| 값 객체, DTO, 이벤트, enum | 미적용 |

`GameRoom` 같은 도메인 모델은 계층이 아니라 계층들이 주고받는 대상이다. 이걸 계층으로 취급하면 규칙이 성립하지 않는다.

## 이름 규칙

implement 계층 객체는 역할을 이름에 드러낸다.

| 접미사 | 역할 |
| --- | --- |
| `Reader` | 조회 + 엔티티 → 도메인 변환 |
| `Writer` / `Appender` / `Updater` | 저장, 수정 |
| `Remover` | 삭제 |
| `Validator` | 검증 |

`SongReader`, `HangmanWordReader`, `ComputerScienceQuizReader`는 이미 이 형태다.

## 패키지 배치

```
com.fungame.songquiz
├── controller/            presentation
├── domain/
│   └── <aggregate>/
│       ├── XxxService.java      service
│       └── implement/           implement
└── storage/               repository
```

`implement`는 도메인의 행위이므로 `domain` 아래에 둔다. 이름을 고정하는 이유는 계층을 읽는 사람에게 드러내기 위한 것이고, ArchUnit이 검사하는 대상은 아니다.

---

# Phase A. 안전망

리팩토링 도중 규칙 위반이 **늘지 않는다**는 것만 기계로 보장한다.

### A-1. ArchUnit 도입 + 현재 위반 동결

기계로 검사하는 것은 **패키지로 드러나는 의존 방향**뿐이다. `controller → domain → storage` 세 방향만 본다.

- `testImplementation 'com.tngtech.archunit:archunit-junit5'`
- `FreezingArchRule`로 현재 위반을 baseline에 기록 → 신규 위반만 실패, 기존 위반은 통과
- baseline 파일(`archunit_store/`)을 커밋

```java
ArchRule rule = FreezingArchRule.freeze(
    layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("controller").definedBy("..controller..")
        .layer("domain").definedBy("..domain..")
        .layer("storage").definedBy("..storage..")
        .whereLayer("controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("domain").mayOnlyBeAccessedByLayers("controller")
        .whereLayer("storage").mayOnlyBeAccessedByLayers("domain"));
```

`service`/`implement`를 별도 계층으로 넣지 않는다. implement는 도메인의 행위이므로 `domain` 패키지 안에 있는 것이 자연스럽고, 이를 ArchUnit 계층으로 쪼개면 논리적인 구분을 패키지 규칙으로 강제하게 된다.

따라서 **Phase C의 `service → implement` 규율은 기계 검사 대상이 아니다.** 리뷰로 지킨다. 기계가 막아주는 것은 Phase B의 역의존뿐이다.

`storage`는 `domain`을 의존하지 않는다는 규칙은 위 계층 규칙이 이미 포함한다(`domain`은 `controller`만 의존할 수 있으므로). 별도 규칙을 두지 않는다.

**완료 기준**: `./gradlew test` 그린. 이후 모든 PR에서 baseline 위반 수가 줄기만 한다.

---

# Phase B. 역의존 끊기

`storage → domain` 화살표를 0으로 만든다. 모듈 분리의 전제 조건이다.

### B-1. 공유 enum 분리

`storage`와 `domain`이 같이 쓰는 enum을 `com.fungame.songquiz.enums`로 옮긴다. 나중에 `core:core-enum` 모듈이 될 패키지다.

| 이동 대상 | 쓰는 곳 |
| --- | --- |
| `Category` | `SongEntity`, `GameRoomEntity` |
| `GameType`, `GameRoomStatus` | `GameRoomEntity` |
| `CSQuizDifficulty` | `ComputerScienceEntity`, `ComputerScienceRepository`, `GameRoomEntity` |
| `Role`, `PlayerStatus`, `PromotionStatus` | member 엔티티 |

조건: 이 패키지는 Spring/JPA 의존을 갖지 않는다. `ActionType`, `ActionResult`처럼 `domain`에서만 쓰는 것은 옮기지 않는다.

**완료 기준**: 패키지 이동뿐이므로 동작 변경 없음. import만 바뀐다.

### B-2. 엔티티에서 변환 로직 제거

`SongEntity.toDomain()` 류를 지우고, 변환을 implement 계층으로 옮긴다.

- `SongEntity` → `SongReader`가 변환
- `ComputerScienceEntity` → `ComputerScienceQuizReader`가 변환
- `GameRoomEntity` → B-3에서 처리

엔티티는 컬럼과 JPA 매핑만 갖는다.

**완료 기준**: `SongEntity`, `ComputerScienceEntity`의 `domain` import 0건.

### B-3. GameRoomStore 해체

`GameRoomStore`는 위치만 `storage`일 뿐 실제로는 implement 계층 객체다. 도메인 변환을 하고 `MemberRepository`까지 조합한다.

- `storage`에는 `GameRoomRepository`, `GameRoomEntity`, `GameRoomMemberEntity`(순수 JPA)만 남긴다
- 변환 + 조합은 `domain/room/implement/GameRoomReader`, `GameRoomWriter`로 옮긴다
- `GameRoomStoreTest`는 새 위치를 따라 이동

가장 위험한 단계다. 이 파일만 단독 PR로 처리한다.

**완료 기준**: `grep -r "import com.fungame.songquiz.domain" storage/` 결과 0건.

### B-4. MemberRepository 이사

`domain/member/MemberRepository`, `PasswordResetTokenRepository`, `PromotionRequestRepository`는 Spring Data 인터페이스인데 domain 패키지에 있다. `storage`로 옮긴다.

**완료 기준**: `domain` 아래에 `extends JpaRepository`가 없다. A-1의 baseline이 비므로 `FreezingArchRule`을 일반 `ArchRule`로 바꿔도 그린이다.

---

# Phase C. 수직 슬라이스 단위로 4계층 적용

애그리거트 하나씩, 작고 테스트가 있는 것부터. 슬라이스마다 PR 하나.

각 슬라이스에서 반복하는 절차:

1. service가 repository를 직접 호출하는 지점을 찾는다
2. 그 호출 + 엔티티 변환을 implement 객체로 추출한다
3. service에는 흐름만 남긴다

Phase B에서 baseline이 이미 비었으므로 이 단계는 ArchUnit이 검사해주지 않는다. 슬라이스마다 1의 지점 수가 줄었는지 PR에서 직접 확인한다.

### C-1. song (파일럿)

`AdminSongController` → `SongService` → `SongReader` / `SongWriter` → `SongRepository`

- 가장 작고 `SongServiceTest`, `SongServiceIntegrationTest`, `SongTest`가 이미 있다
- `SongService`가 `SongRepository`를 직접 의존하는 부분을 `SongReader`/`SongWriter` 뒤로 보낸다
- 여기서 정한 패턴이 이후 슬라이스의 템플릿이 된다

### C-2. member / auth

대상: `AuthService`, `MemberPresenceService`, `PasswordResetService`, `PromotionService`, `UserDetailsServiceImpl`

- `MemberReader`, `MemberWriter`, `PasswordResetTokenReader/Writer`, `PromotionRequestReader/Writer` 추출
- `MemberAdapter`는 이미 변환기 성격이므로 implement로 이동
- 결정 필요: `UserDetailsServiceImpl`은 스프링 시큐리티 콜백이다. service 계층에 두고 implement를 의존하게 한다

### C-3. invite / promotion

대상: `RoomInviteService`(146줄)

- `RoomInviteService`가 방과 회원을 함께 다루므로, C-2의 `MemberReader`를 재사용하는 첫 사례가 된다
- implement가 implement를 의존하는 예외 규칙이 여기서 처음 쓰인다

### C-4. room / game

가장 큰 덩어리라 마지막에 한다. 이 안에서도 쪼갠다.

| 순서 | 대상 | 비고 |
| --- | --- | --- |
| C-4a | `GameRoomManager`(215줄) | 이미 implement 성격 → implement로 이동 |
| C-4b | `GameRoomService`(133줄) | 흐름만 남기고 조회/저장은 B-3의 Reader/Writer로 |
| C-4c | `PlayerService` | |
| C-4d | `QuizGameService`(178줄), `HangmanGameService`(147줄) | 게임 엔진(`GameRoom`, `HangmanGame`, `GamePlayers`)은 도메인 모델이므로 계층 규칙 대상이 아니다 |
| C-4e | `GameSessionManager`, `GameServiceRouter` | |

**Phase C 완료 기준**: service가 repository를 직접 의존하는 파일이 0개.

---

# Phase D. 물리 모듈 분리

Phase C까지 끝나면 의존 방향이 이미 맞으므로, 여기서는 파일을 옮기고 build.gradle을 쓰는 일만 남는다.

### D-1. 멀티 모듈 껍데기 전환

- `backend/settings.gradle`에 `include 'core:core-api'` 하나만
- 모든 코드를 `core/core-api`로 통째 이동
- 루트 `build.gradle`에 `subprojects {}` 공통 설정
- 이 단계에서 동작이 안 바뀌는지 확인 (`./gradlew bootRun` + 인수 테스트)

### D-2. core:core-enum 추출

B-1에서 만든 `enums` 패키지를 그대로 모듈로 승격. 의존 없음.

### D-3. storage:db-core 추출

- `storage` 패키지 + `resources/db/migration`(flyway) + `db/local` 이동
- `implementation project(':core:core-enum')`만 의존
- QueryDSL annotationProcessor를 이 모듈로 이동
- Testcontainers 기반 `MySqlIntegrationTest`, `MySqlTestContainer` 이동

### D-4. clients 추출

- `support/extern/YoutubeScraper` → `clients:client-youtube`
- `HangmanRandomWordApiProvider` → `clients:client-random-word`
- **선행 작업**: 두 클래스가 도메인 타입(`Song`, `HangmanWordProvider`)을 직접 반환하지 않도록 자체 응답 모델로 바꾸고, 도메인 변환은 `core-api`가 맡는다. clients는 어떤 프로젝트 모듈도 의존하지 않아야 한다
- `YoutubeScraperTest`는 `external` 태그가 붙어 있으므로 태그 설정도 함께 이동

### D-5. support 추출

- `support:logging`, `support:monitoring` 신설 (logback, actuator)
- `support/mail`(SES) → `support:mail`. `PasswordResetMailListener`는 이벤트 리스너라 `core-api`에 남기고, 발송기 인터페이스/구현만 모듈로 뺀다

### D-6. tests:api-docs 추출

`RestDocsSupport`를 모듈로. `core-api`의 `testImplementation`으로만 붙인다.

---

## 모듈 분리 시 주의

- **컴포넌트 스캔**: 패키지 루트를 `com.fungame.songquiz`로 유지한다. 템플릿처럼 `com.fungame.songquiz.storage.db.core`로 바꾸면 `@SpringBootApplication` 스캔 범위 밖이 되어 각 모듈에 `@Configuration`, `@EnableJpaRepositories`, `@EntityScan`을 명시해야 한다. 어느 쪽을 택할지 D-1에서 결정한다
- **flyway**: 마이그레이션 리소스가 `db-core`로 가면 `core-api` 실행 시 클래스패스에 포함되는지 확인
- **spring-session-jdbc**: 세션 테이블 DDL이 flyway와 같은 모듈에 있어야 한다
- **application.yml**: 모듈별 `application-*.yml`을 어떻게 병합할지 D-1에서 정한다

## 진행 지표

| 지표 | 현재 | 목표 |
| --- | --- | --- |
| `storage → domain` import | 5개 파일 | 0 |
| service가 repository 직접 의존 | 14개 파일 | 0 (리뷰로 확인) |
| ArchUnit freeze 위반 수 | 92 | 0 |
| gradle 모듈 수 | 1 | 9 |

## PR 운영

- PR 하나 = 위 단계 하나. 여러 단계를 묶지 않는다
- 모든 PR은 머지 시점에 배포 가능해야 한다. 중간에 깨진 상태를 남기지 않는다
- Phase B-3, D-1, D-3은 단독 PR로 처리한다
