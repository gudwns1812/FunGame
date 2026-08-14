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

baseline은 위반을 **텍스트로** 매칭한다. 메서드 시그니처에 든 타입의 패키지가 바뀌거나 줄 번호가 밀리면 같은 위반이 "신규"로 잡혀 실패한다. B-1에서 실제로 밟았다. 이럴 때는 옛 baseline과 대조해 회귀가 아님을 확인한 뒤 refreeze하고, 새 baseline이 옛 baseline의 부분집합인지 검증한다.

B-3에서 위반이 0이 되어 일반 `ArchRule`로 바꿨고 baseline은 지웠다. 아래 스니펫은 그 시점 이전의 모습이다.

```java
ArchRule rule =
    layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("controller").definedBy("..controller..")
        .layer("domain").definedBy("..domain..")
        .layer("storage").definedBy("..storage..")
        .layer("enums").definedBy("..enums..")
        .whereLayer("controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("domain").mayOnlyBeAccessedByLayers("controller")
        .whereLayer("storage").mayOnlyBeAccessedByLayers("domain")
        .whereLayer("enums").mayNotAccessAnyLayer();
```

`enums` 계층은 B-1에서 추가됐다.

`service`/`implement`를 별도 계층으로 넣지 않는다. implement는 도메인의 행위이므로 `domain` 패키지 안에 있는 것이 자연스럽고, 이를 ArchUnit 계층으로 쪼개면 논리적인 구분을 패키지 규칙으로 강제하게 된다.

따라서 **Phase C의 `service → implement` 규율은 기계 검사 대상이 아니다.** 리뷰로 지킨다. 기계가 막아주는 것은 Phase B의 역의존뿐이다.

`storage`는 `domain`을 의존하지 않는다는 규칙은 위 계층 규칙이 이미 포함한다(`domain`은 `controller`만 의존할 수 있으므로). 별도 규칙을 두지 않는다.

**완료 기준**: `./gradlew test` 그린. 이후 모든 PR에서 baseline 위반 수가 줄기만 한다. (B-3에서 0에 도달해 freeze를 풀었다.)

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
| `ActionType`, `ActionResult` | `domain`만 (일관성 때문에 함께 이동) |

조건: 이 패키지는 Spring/JPA 의존을 갖지 않는다.

`ActionType`, `ActionResult`는 `storage`가 쓰지 않아 모듈 분리와는 무관하지만, enum이 두 곳에 흩어져 있는 것이 읽기 나쁘므로 함께 옮긴다. 둘 다 의존이 없어 전제를 깨지 않는다.

`support`의 `ErrorType`, `ErrorCode`, `ResultType`은 옮기지 않는다. `ErrorType`이 `HttpStatus`, `LogLevel`, `domain.member.PasswordPolicy`를 의존하기 때문이다.

- Spring 의존이 들어오면 D-2의 `core:core-enum`이 spring-web을 지고 가야 한다
- `PasswordPolicy` 의존은 `enums → domain` 역의존이라 A-1 규칙 위반이다
- `ErrorCode`만 떼면 에러 enum이 두 패키지로 갈라져 오히려 더 흩어진다

세 개는 상수 모음이 아니라 HTTP에 묶인 에러 카탈로그다. D-5에서 `support`가 모듈로 갈라질 때 함께 간다.

되돌아가지 않도록 A-1의 계층 규칙에 `enums` 계층을 추가하고 `mayNotAccessAnyLayer()`를 건다. Lombok은 `compileOnly`라 남겨도 된다. D-2에서 이 규칙은 모듈 경계로 대체된다.

패키지 이동이 배포 시 깨질 수 있는 지점을 확인했다.

- JPA: 모두 `@Enumerated(EnumType.STRING)`이라 DB에는 이름만 들어간다. 패키지와 무관하다
- spring-session-jdbc: 세션에 직렬화되는 `MemberAdapter`는 `Long`/`String` 필드만 갖고, 권한은 `Role.getKey()`로 만든 `SimpleGrantedAuthority` 문자열이다. `Role` 인스턴스가 세션 바이트에 들어가지 않으므로 기존 세션이 깨지지 않는다

**완료 기준**: 패키지 이동뿐이므로 동작 변경 없음. import만 바뀐다.

### B-2. 엔티티에서 변환 로직 제거

`SongEntity.toDomain()` 류를 지우고, 변환을 implement 계층으로 옮긴다.

- `SongEntity` → `SongReader`가 변환
- `ComputerScienceEntity` → `ComputerScienceQuizReader`가 변환
- `GameRoomEntity` → B-3에서 처리

엔티티는 컬럼과 JPA 매핑만 갖는다.

`SongReader`에는 이미 `SongEntity.toDomain()`과 같은 일을 하는 private `toDomain`이 있었다. 새로 만드는 것이 아니라 중복을 지우고 호출부를 한쪽으로 모으는 일이다.

옮기기 전에 매핑을 테스트로 덮는다. 기존 `ComputerScienceQuizReaderTest`는 개수와 난이도만 보고 필드 매핑을 검증하지 않았고, `SongReader`는 테스트가 없었다. 변환을 옮기는 리팩토링에서 필드를 잘못 짚어도 아무 테스트도 실패하지 않는 상태였다.

- `SongReaderTest` 신설. 세 조회 경로(`findById`, `findSongWithCount`, `findSongByCategoryWithCount`) 모두에서 8개 필드를 검증한다
- `ComputerScienceQuizReaderTest`에 5개 필드 매핑 검증 추가

엔티티와 도메인의 이름이 다른 지점이 있어 특히 필요하다: `videoLink` → `link`, `content` → `question`, `explanation` → `explain`. `Song.of`가 제목을 정답 집합에 넣는 것도 함께 검증한다.

**완료 기준**: `SongEntity`, `ComputerScienceEntity`의 `domain` import 0건. ArchUnit baseline 45 → 41.

### B-3. GameRoomStore 해체

`GameRoomStore`는 위치만 `storage`일 뿐 실제로는 implement 계층 객체다. 도메인 변환을 하고 `MemberRepository`까지 조합한다.

- `storage`에는 `GameRoomRepository`, `GameRoomEntity`, `GameRoomMemberEntity`(순수 JPA)만 남긴다
- 변환 + 조합은 `GameRoomReader`, `GameRoomWriter`로 옮긴다
- `GameRoomStoreTest`는 새 위치를 따라 이동

가장 위험한 단계다. 이 파일만 단독 PR로 처리한다.

`GameRoomEntity`가 `RoomSettings`와 `GamePlayer`를 알고 있어 이것도 함께 끊어야 한다. `RoomSettings`는 `toGameCreateInfo()`로 `gamecreator`를 의존하므로 `enums`로 내릴 수 없는 도메인 타입이다. 엔티티가 쓸 자기 모양을 중첩 레코드로 두고, 도메인 쪽 Reader/Writer가 번역한다. D-4에서 clients에 적용할 방식과 같다.

- `GameRoomEntity.Settings` — 설정 일곱 컬럼
- `GameRoomEntity.MemberState` — `memberId`, `ready`. 닉네임은 저장하지 않는다

위치는 `domain/room/implement/`가 아니라 평평한 `domain`이다. `SongReader`, `ComputerScienceQuizReader`, `HangmanWordReader`가 모두 그렇고, 애그리거트 패키지 분리는 Phase C/D의 일이다.

주의한 지점:

- **트랜잭션 경계**. `save`는 조회 후 엔티티를 고치고 `save`를 부르지 않는다. 더티 체킹에 기대므로 `@Transactional`이 반드시 같은 메서드에 있어야 한다. `GameRoomServiceTransactionBoundaryTest`가 지킨다
- **지연 로딩**. `findAllBy`, `findWithMembersById`는 `@EntityGraph(attributePaths = "members")`라 members가 즉시 로딩된다. 그래서 변환을 트랜잭션 안에서 하든 밖에서 하든 안전하다. 이 애노테이션을 떼면 `LazyInitializationException`이 난다

옮기기 전에 `GameRoomStoreTest`를 보강했다. 기존 3개는 `loadAll`, `markInterruptedGamesWaiting`, `delete`, 준비 상태 동기화를 덮지 않았고 설정도 일곱 필드 중 넷만 봤다. 두 개의 `int`(`totalRound`, `difficulty`)가 붙어 있어 번역에서 뒤바뀌어도 잡히지 않는 상태였다.

- 설정 일곱 필드 왕복을 `open` 경로와 `save` 경로 각각에서 레코드 전체 비교로 검증
- `loadAll`, `delete`, `markInterruptedGamesWaiting` 추가
- 준비 상태 변경과 나간 참가자가 함께 반영되는지 추가

이 클래스는 `@SpringBootTest`이고 롤백이 없어 DB가 테스트 간 공유된다. `loadAll`은 전체 개수 대신 자기가 만든 방만 골라 단정한다.

**완료 기준**: `grep -r "import com.fungame.songquiz.domain" storage/` 결과 0건.

### B-4. MemberRepository 이사

`domain/member/MemberRepository`, `PasswordResetTokenRepository`, `PromotionRequestRepository`는 Spring Data 인터페이스인데 domain 패키지에 있다. `storage`로 옮긴다.

B-3에서 `GameRoomStore`가 사라지면서 `storage → domain.member.MemberRepository` 위반도 함께 없어졌다. 지금 `MemberRepository`를 쓰는 것은 `domain`의 `GameRoomReader`뿐이고 `domain → domain`은 허용된 방향이다. 그래서 **B-4는 ArchUnit이 잡아주는 위반이 아니라 배치 문제다**. Spring Data 인터페이스가 `domain`에 있는 것을 정리하는 일이고, D-3에서 `storage:db-core` 모듈을 뗄 때 필요해진다.

**완료 기준**: `domain` 아래에 `extends JpaRepository`가 없다.

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

`backend`는 독립 Gradle 빌드가 아니라 루트 `FunGame` 빌드의 하위 프로젝트다. 그래서 `backend/settings.gradle`을 새로 만들지 않고 루트 `settings.gradle`을 고친다.

```groovy
rootProject.name = 'FunGame'
include 'backend:core:core-api'
```

- `backend/src` → `backend/core/core-api/src`
- `backend/build.gradle` → `backend/core/core-api/build.gradle` (내용 그대로)
- Gradle 경로가 `:backend`에서 `:backend:core:core-api`로 바뀐다

`subprojects {}` 공통 설정은 만들지 않는다. 모듈이 하나뿐이라 공유할 것이 없다. D-2에서 두 번째 모듈이 생길 때 뽑았다(`backend/build.gradle`).

**컴포넌트 스캔**: 패키지 루트를 `com.fungame.songquiz`로 유지한다. D-1은 디렉터리만 옮기고 패키지는 건드리지 않으므로 `@SpringBootApplication` 스캔 범위가 그대로다. `@EntityScan`, `@EnableJpaRepositories`를 명시할 필요가 없다.

**application.yml**: 모든 리소스가 `core-api`에 그대로 있어 아직 병합할 것이 없다. flyway 리소스가 갈라지는 D-3에서 정한다.

Gradle 경로가 바뀌면 배포가 조용히 깨질 수 있는 곳들을 함께 고친다.

| 파일 | 고칠 것 |
| --- | --- |
| `backend/Dockerfile` | `COPY` 경로 3곳, gradle 태스크 2곳, jar 산출 경로 |
| `.github/workflows/ci-backend.yml` | 테스트 태스크, 리포트 업로드 경로 |
| `.github/workflows/deploy-backend.yml` | 테스트 태스크 |
| `backend/.gitignore` | `/src/test/resources/...` 앵커 경로 |
| `README.md`, `application-local.yml` | `bootRun` 명령 |

`:backend:test`는 태스크가 사라져 **BUILD FAILED**가 된다. 조용히 통과하는 no-op이 아니므로 CI에서 놓칠 수 없다. 이걸 확인하는 것이 이 단계의 핵심이다.

jar 이름이 프로젝트 이름을 따라 `core-api-0.0.1-SNAPSHOT.jar`로 바뀐다. Dockerfile이 `*.jar` 글롭이라 문제없다.

**완료 기준**: `./gradlew :backend:core:core-api:test` 그린, `bootJar` 산출 경로가 Dockerfile과 일치, `:backend:test`는 실패.

### D-2. core:core-enum 추출

B-1에서 만든 `enums` 패키지를 그대로 모듈로 승격. 의존 없음.

```groovy
rootProject.name = 'FunGame'
include 'backend:core:core-enum'
include 'backend:core:core-api'
```

- `core-api/src/main/java/.../enums` → `core-enum/src/main/java/.../enums` (파일 이동만)
- `core-api`에 `implementation project(':backend:core:core-enum')` 추가
- `core-enum`은 build.gradle이 없다. 선언할 것이 없고 공통 설정이 java 플러그인을 준다

D-1에서 미뤄둔 공통 설정을 여기서 `backend/build.gradle`로 뽑는다. 모듈이 둘이 되어 공유할 것이 생겼다.

**Lombok 버전을 한 곳에 고정한다.** `Role`, `PromotionStatus`가 `@Getter`를 쓰므로 `core-enum`도 Lombok이 필요한데, 이 모듈은 Boot 플러그인을 쓰지 않아 Boot BOM의 버전 관리를 받지 못한다. `org.projectlombok:lombok`을 버전 없이 쓰면 해석에 실패한다. Boot 3.4.3이 관리하던 값을 `backend/build.gradle`의 `lombokVersion`에 두고 두 모듈이 함께 쓴다. Boot를 올릴 때 이 값도 확인해야 한다.

Lombok은 `compileOnly`라 `core-enum`의 런타임 의존은 비어 있다(`runtimeClasspath` → `No dependencies`). ARCHITECTURE.md의 "의존 없는 최하위 모듈"을 지킨다.

여기서 검증이 두 겹이 된다.

- ArchUnit은 여전히 `core-enum` jar 안의 클래스를 본다. 계층 규칙에 `withOptionalLayers`를 주지 않았으므로, 못 보면 "Layer 'enums' is empty"로 실패한다. 패키지를 없는 이름으로 바꿔 실패하는 것을 확인했다
- 이제 **컴파일러가 먼저 막는다.** `core-enum`은 `core-api`를 의존하지 않으므로 `enums → domain`을 쓰면 `package com.fungame.songquiz.domain does not exist`로 빌드가 깨진다. 규칙이 테스트가 아니라 모듈 그래프로 강제된다

**완료 기준**: `core-enum`의 `runtimeClasspath`가 비어 있고, `core-api` 테스트 그린, `bootJar` 정상.

### D-3. storage:db-core 추출

- `storage` 패키지 + `resources/db/migration`(flyway) + `db/local` 이동
- `implementation project(':core:core-enum')`만 의존
- QueryDSL annotationProcessor를 이 모듈로 이동
- Testcontainers는 `testFixtures`로 내보낸다

**flyway는 db-core만 갖는다.** 마이그레이션이 만드는 테이블이 전부 `storage` 엔티티 것이다. `classpath:db/migration`은 중첩 jar를 포함한 클래스패스 전체에서 해석되므로 `core-api` 실행에도 문제가 없다. jar 안에 실제로 들어가는지 확인해야 한다(`BOOT-INF/lib/db-core-*.jar` 안의 `db/migration/`).

문서에 있던 "spring-session-jdbc 세션 테이블 DDL이 flyway와 같은 모듈에 있어야 한다"는 **틀린 걱정이었다.** 마이그레이션에 `SPRING_SESSION`이 없다. 세션 테이블은 spring-session-jdbc 자체 스키마 스크립트가 `initialize-schema`로 만든다.

#### 의존성 분리와 JPA

`core-api`의 Reader/Writer가 리포지토리를 직접 쓴다. `findById`, `save` 같은 **Spring Data 상속 메서드**를 부르려면 javac가 타입 계층을 해석해야 하므로 spring-data-jpa가 컴파일 클래스패스에 있어야 한다. 그래서 `db-core`가 이것만 `api`로 공개한다.

```groovy
api 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
implementation querydsl
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'com.h2database:h2'
```

`core-api`에서 flyway, mysql-connector-j, querydsl, h2가 사라진다.

도메인에 인터페이스를 두고 storage가 구현하는 방식(DIP)은 이 모듈 그래프에서 쓸 수 없다. 구현체가 도메인 모델을 알아야 해서 `db-core → core-api`가 되고, Phase B에서 없앤 역의존이 되살아나 순환이 된다. 문제는 인터페이스 위치가 아니라 **누가 Spring Data를 호출하느냐**다.

Spring Data를 상속하지 않는 위임 클래스를 `db-core`에 두면 JPA를 완전히 숨길 수 있다. 실제로 해봤고 동작한다(누수는 상속 CRUD 여섯 개뿐이었다). 다만 리포지토리 메서드를 늘릴 때마다 위임도 늘어나고, 그 대가가 얻는 것보다 크다고 판단해 되돌렸다.

#### 설정 파일

DB 설정은 `db-core/src/main/resources/db-core.yml`이 갖는다. `core-api`가 가져간다.

```yaml
spring:
  config:
    import: classpath:db-core.yml
```

`datasource.hikari`, `flyway.baseline-*`, `jpa.hibernate.ddl-auto`, 그리고 로컬 프로파일의 H2 설정과 `flyway.locations`가 여기로 왔다. 가져오는 쪽이 우선순위가 높으므로 `application.yml`이나 프로파일 파일에서 덮어쓸 수 있다.

로컬 프로파일은 별도 파일이 아니라 같은 파일의 두 번째 문서(`spring.config.activate.on-profile: local`)로 뒀다. `spring.config.import`가 프로파일별 변형 파일을 자동으로 찾는지 확인하지 못했고 로컬 프로파일은 테스트가 없어서다.

`optional:` 접두어를 쓰지 않았으므로 파일이 없으면 기동이 `ConfigDataResourceNotFoundException`으로 실패한다. 파일을 잠시 치워 실제로 실패하는 것을 확인했다. 조용히 무시되지 않는다.

`spring.session.jdbc.initialize-schema`는 세션이 웹 관심사라 `core-api`에 남긴다.

#### 테스트

`@MySqlIntegrationTest`를 `@IntegrationTest`로 바꾼다. 이 애노테이션을 쓰는 12개 클래스 중 MySQL이어야만 하는 것은 넷뿐이다.

| 이유 | 클래스 |
| --- | --- |
| `JSON_CONTAINS`, `ORDER BY RAND()` | `SongServiceIntegrationTest`, `AdminSongControllerTest` |
| `engine = InnoDB`, `enum(...)`, `json` DDL | `FlywayMigrationTest` |
| `innodb-lock-wait-timeout` + `PESSIMISTIC_WRITE` | `PasswordResetConcurrencyTest` |

나머지 여덟은 "앱 컨텍스트 + DB"가 필요할 뿐이다. 이 애노테이션이 제공하는 것은 MySQL이 아니라 **실제 DB를 붙인 통합 테스트**이고, MySQL은 그 DB를 조달하는 방법이다.

통합 테스트는 픽스처를 준비하려고 리포지토리를 직접 쓴다. `testFixturesApi`로 테스트 스코프에만 JPA를 열고 프로덕션 클래스패스는 건드리지 않는다. 프로덕션 `Store`에 테스트 전용 `deleteAll` 같은 메서드를 넣지 않기 위해서다.

`@Mock`으로 리포지토리를 쓰던 단위 테스트 셋은 `Store`로 바꿔야 한다. **컴파일은 통과하고 런타임에만 터진다** — 옛 타입이 여전히 참조 가능하므로 `@Mock`은 만들어지지만 `@InjectMocks`가 주입할 곳이 없어 필드가 null로 남는다.

**완료 기준**: `core-api` 컴파일 클래스패스에 flyway·mysql-connector-j·querydsl이 없다. `bootJar` 안의 `db-core` jar에 `db/migration/`과 `db-core.yml`이 있다.

### D-4. clients 추출

`support/extern/YoutubeScraper` → `clients:client-youtube`.

선행 작업은 [곡 등록 비동기화](#곡-등록-비동기화)에서 끝났다. `YoutubeScraper`는 이제 `Optional<String>`만 돌려주고 jsoup, Spring, slf4j 만 의존한다. 파일 이동과 build.gradle 작성만 남았다.

`YoutubeScraperTest`는 `external` 태그가 붙어 있어 태그 제외 설정도 함께 옮긴다.

**`clients:client-random-word`는 만들지 않는다.** `HangmanRandomWordApiProvider`는 이름과 달리 외부 API 를 호출하지 않는다. `words/difficulty_1~4.txt` 를 `ClassPathResource` 로 읽어 `@PostConstruct` 에서 메모리에 올리고 `Random` 으로 뽑는다. HTTP 호출이 한 줄도 없다.

모듈로 빼면 없는 클라이언트를 위한 모듈이 되고, 게다가 `HangmanWordProvider`(domain 인터페이스)를 구현하고 `CoreException`, `ErrorType`(support)를 쓰므로 `clients → core-api` 역의존이 생긴다. "clients 는 다른 프로젝트 모듈을 의존하지 않는다"와 충돌한다.

진짜 문제는 모듈이 아니라 **단어를 코드에 번들해 둔 것**이었다. 그래서 이름을 고치는 대신 단어를 DB 로 옮겼다.

- `V11__hangman_word.sql` 이 `hangman_word` 테이블을 만들고 단어 3668개를 넣는다
- `HangmanWordReader` 가 `ORDER BY RAND() LIMIT 1` 로 뽑는다
- `HangmanRandomWordApiProvider`, `HangmanWordProvider`, `words/*.txt` 를 지웠다

`march` 가 난이도 1과 3에 중복이라 unique 키는 `(word, difficulty)` 다. `word` 단독으로 걸면 마이그레이션이 실패하고 어느 난이도를 버릴지 정해야 한다. 현재 동작을 그대로 두는 쪽을 택했다.

기존 `HangmanWordReaderTest` 는 공급자를 mock 해서 "받은 단어로 게임을 만든다" 만 봤다. 이제 검증할 것이 없으므로 통합 테스트로 바꿔 **난이도별 적재 개수**를 단정한다. 생성된 `insert` 39문 중 일부만 들어가도 잡히지 않으면 모른다.

---

# 곡 등록 비동기화

계획에 없던 작업이지만 D-4 의 선행 조건과 겹치므로 여기에 남긴다.

관리자가 곡을 저장하면 요청 경로에서 유튜브를 긁고 있었다.

- Jsoup 타임아웃이 없어 기본 30초까지 매달린다
- 스크랩이 실패하면 예외를 삼켜 `"Error: connect timed out"` 이나 빈 문자열을 `video_link` 에 저장한다. `not null` 이라 제약도 걸리지 않아 재생 불가 행이 조용히 쌓인다
- 중복 검사를 스크랩 뒤에 해서 중복이라도 30초를 먼저 쓴다

저장을 두 단계로 나눴다.

1. `createSongQuiz` 는 중복만 검사하고 `song_scrape_request` 에 넣고 끝낸다. 외부 호출이 없다
2. 스케줄러가 대기 행을 꺼내 스크랩하고, 얻은 유튜브 id 로 `song_entity` 에 `INSERT ... ON DUPLICATE KEY UPDATE` 한다. 성공하면 대기 행을 지운다

스크랩은 트랜잭션 밖에서 한다. 안에서 하면 커넥션을 쥔 채 네트워크를 기다린다. DB 쓰기 둘은 각각 별도 빈의 짧은 트랜잭션이고, 중간에 실패해도 upsert 가 멱등이라 다음 회차에 다시 처리된다.

`@Scheduled(fixedDelay)` 는 기동 직후 한 번 돈다. 통합 테스트가 대기 행을 남기면 실제 유튜브를 치므로 트리거를 `SongScrapeScheduler` 로 떼고 `app.song-scrape.enabled` 로 끌 수 있게 했다. `@IntegrationTest` 가 끈다.

## 운영 스키마가 flyway 와 어긋나 있었다

`song_entity` 의 unique 제약 셋과 인덱스 둘이 운영에만 손으로 붙어 있고 마이그레이션에는 없었다. `ddl-auto: validate` 는 제약과 인덱스를 검사하지 않아 드러나지 않았다.

그대로 두면 테스트와 로컬에는 `video_link` unique 가 없어 **upsert 가 그냥 insert 로 동작한다.** 검증할 수 없는 코드가 된다.

| 마이그레이션 | 내용 |
| --- | --- |
| `V9__song_entity_constraints.sql` | `unique (singer, title)`, `unique (video_link)`, `uq_title_date`, `idx_answers` |
| `mysql/V10__song_entity_category_index.sql` | 다중값 함수 인덱스. MySQL 전용이라 `db/migration/{vendor}` 로 분리 |
| `db/manual/mark_V9_V10_applied.sql` | 운영은 이미 제약이 있으므로 실행하지 않고 적용된 것으로만 기록 |

V9 의 `singer`, `video_link` 제약은 이름을 붙이지 않았다. MySQL 이 첫 컬럼 이름으로 자동 명명하므로 운영에 있는 이름이 그대로 재현된다.

checksum 은 손으로 쓰지 않는다. Flyway 알고리즘(줄별 개행 제거 후 CRC32 누적)으로 계산한 뒤 빈 DB 에 실제로 적용해 기록된 값과 대조한다. 기존 V1~V7 로 알고리즘이 맞는지 먼저 확인할 수 있다.

**배포 전에 `mark_V9_V10_applied.sql` 을 운영에 먼저 실행해야 한다.** 안 하면 flyway 가 이미 있는 제약을 또 만들려다 실패해 기동이 안 된다. 실행 전에 `SHOW INDEX FROM song_entity` 로 제약 다섯 개가 실제로 있는지 확인한다. 하나라도 없으면 실행하면 안 된다 — 그 제약이 영구히 누락된다.

## 중복 검사는 DB 제약과 같은 키를 덮어야 한다

`(singer, title)` 만 보면 제목과 발매일이 같고 가수만 다른 곡이 통과한다. 그러면 upsert 가 `video_link` 가 아니라 `uq_title_date` 에 먼저 걸려 **엉뚱한 행을 갱신한다.** `ON DUPLICATE KEY UPDATE` 는 어느 unique 키에 걸렸는지 구분하지 않는다.

초안 저장 시점에서 네 가지를 모두 본다. 대기 테이블에도 두 unique 를 걸어 경쟁 상황을 막았다.

## 남은 것

기존 오염 데이터를 정리하지 않았다. 이번 변경은 새로 들어오는 것만 막는다.

```sql
SELECT id, title, singer, video_link FROM song_entity
WHERE video_link = '' OR video_link LIKE 'Error:%';
```

---

### D-4b. clients:client-mail 추출

메일은 원래 `support:mail`로 계획돼 있었지만 AWS SES 를 호출하므로 `clients`가 맞다. `support:logging`, `support:monitoring`은 외부 호출이 없는 설정 모듈이고 SES 는 제3자 API 다. `client-youtube`와 성격이 같다.

`support/mail` 다섯 파일이 세 역할로 섞여 있었다.

| 파일 | 간 곳 |
| --- | --- |
| `SesClientConfig`, SES 발송 코드 | `clients:client-mail` |
| `PasswordResetMailSender`(포트), `LoggingPasswordResetMailSender` | `core-api` |
| `PasswordResetMailListener` | `core-api` |

`SesPasswordResetMailSender`가 `PasswordResetTokenGenerator.TOKEN_TTL`을 참조해 메일 본문에 "몇 분 뒤 만료"를 쓰고 있었다. 그대로 옮기면 `clients → core-api` 역의존이다. 그래서 클라이언트는 `send(to, subject, body)` 만 아는 `SesMailSender` 로 두고, 제목과 본문 조립은 `core-api` 어댑터가 맡는다. D-3의 위임 문제와 같은 구조다.

설정도 모듈이 갖는다. `client-mail.yml` 이 `client.mail.aws-region`, `client.mail.from` 을 갖고 `application.yml` 이 `spring.config.import` 로 가져간다. **환경변수 이름(`AWS_REGION`, `MAIL_FROM`)은 그대로라 배포 설정은 바뀌지 않는다.**

SES 경로는 `@Profile("prod")` 라 테스트가 태우지 않는다. 옮기기 전에도 그랬다. 본문 조립이 옮겨갔으므로 어댑터 단위 테스트로 링크와 만료 시간이 본문에 들어가는지 검증한다. **SES 빈 배선 자체는 여전히 미검증이다.**

### D-5. support 추출

- `support:logging`, `support:monitoring` 신설 (logback, actuator)

### D-6. tests:api-docs 추출

`RestDocsSupport`를 모듈로. `core-api`의 `testImplementation`으로만 붙인다.

---

## 모듈 분리 시 주의

- **컴포넌트 스캔**: 패키지 루트를 `com.fungame.songquiz`로 유지한다. 템플릿처럼 `com.fungame.songquiz.storage.db.core`로 바꾸면 `@SpringBootApplication` 스캔 범위 밖이 되어 각 모듈에 `@Configuration`, `@EnableJpaRepositories`, `@EntityScan`을 명시해야 한다. 어느 쪽을 택할지 D-1에서 결정한다
- **flyway**: 마이그레이션 리소스가 `db-core`로 가면 `core-api` 실행 시 클래스패스에 포함되는지 확인
- **application.yml**: 모듈별 `application-*.yml`을 어떻게 병합할지 D-1에서 정한다

## 진행 지표

| 지표 | 현재 | 목표 |
| --- | --- | --- |
| `storage → domain` import | **0** (A-1 시점 5개 파일) | 0 |
| service가 repository 직접 의존 | 14개 파일 | 0 (리뷰로 확인) |
| ArchUnit 위반 수 | **0** (A-1 시점 92) | 0 |
| gradle 모듈 수 | 3 (`core:core-api`, `core:core-enum`, `storage:db-core`) | 9 |

ArchUnit 위반이 0이 되어 B-3에서 `FreezingArchRule`을 일반 `ArchRule`로 바꿨다. `archunit_store/`와 `archunit.properties`는 지웠다. 이제 위반이 하나라도 생기면 바로 실패한다.

## PR 운영

- PR 하나 = 위 단계 하나. 여러 단계를 묶지 않는다
- 모든 PR은 머지 시점에 배포 가능해야 한다. 중간에 깨진 상태를 남기지 않는다
- Phase B-3, D-1, D-3은 단독 PR로 처리한다
