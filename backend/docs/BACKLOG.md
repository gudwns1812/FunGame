# BACKLOG

리팩토링 도중 확인했지만 범위 때문에 미뤄둔 것들. 각 항목은 "무엇을, 왜 지금 안 했는지, 어디를 보면 되는지"만 적는다.

---

## 구조

### 1. ArchUnit 규칙 군살 빼기

`LayerDependencyTest`의 네 레이어 중 `storage`, `enums`는 Gradle 모듈 그래프가 이미 컴파일 타임에 막는다. `db-core`는 `core-enum`만 의존하므로 `core-api`를 볼 수조차 없다. 반면 `controller`와 `domain`은 둘 다 `core-api` 안에 있어 모듈이 못 막는다.

- 남길 것: `domain`이 `controller`를 몰라야 한다
- 뺄 것: `storage`, `enums` 레이어
- 더할 것: `support`(error + config)를 모든 레이어가 접근 가능한 공유 커널로 선언

**실효성은 확인됐다.** 도메인 재배치 중 `RoomPresence`를 controller로 잘못 내렸을 때 이 테스트가 `GameRoomService → RoomPresence` 역방향을 잡아냈다.

### 2. client 모듈 의존 (결론: 현행 유지)

`core-api → clients:*`는 `ARCHITECTURE.md`가 의도한 구조다. 이걸 끊으려면 `clients`가 `core-api`를 의존해야 하는데, 그러면 조립을 맡을 제3 모듈이 필요하다(Gradle이 순환을 거부한다). 그 모듈은 `SongquizApplication`을 가져가므로 **`@SpringBootTest` 통합테스트 15개가 부팅 지점을 잃고**, Dockerfile 3줄이 바뀐다. 비용 대비 이득이 없어 원래대로 뒀다.

---

## 중복

### 3. nickname 3중 비정규화

`GamePlayer`, `GameRank`(별도 `Map<Long, String> nicknames`), `RoomMember`가 각각 nickname을 들고 있다. 방은 memberId만 영속하므로 `GameRoomReader`가 방을 읽을 때마다 `findNicknames()`로 손 조인을 하고, nickname이 null인 플레이어를 걸러낸다.

### 4. `StoredRoom`과 `GameRoom` 통합

`GameRoom.game` 필드는 **쓰기 전용 죽은 상태**다. `start()`에서 대입하고 `finishGame()`에서 null로 되돌릴 뿐 어디서도 읽지 않는다(`isPlaying()`은 `status`를 본다). 이 필드를 지우면 `StoredRoom` = `GameRoom` + `roomId`가 되어 한 타입으로 합칠 수 있다. `GameRoom.restore(settings, players, hostId, lastActivityTime)`가 이미 `StoredRoom`의 필드 그대로다.

### 5. `Game`과 `GameRank`의 플레이어 명단 이중 관리

`GameSession` 생성 시 `game.setPlayers(players)`와 `new GameRank(players)`로 각자 명단을 든다. `removePlayer`/`restorePlayer`가 양쪽에 따로 통보한다. aggregate를 나눈 지금은 이 중복이 경계에 드러나 있다.

---

## 죽은 코드

### 6. 참조 0건

- `GameSkipInfo` — 쓰는 곳 없음
- `GameServiceRouter` — 쓰는 곳 없음

---

## 이름과 타입

### 7. DTO가 아닌데 DTO인 것들

폴더는 aggregate로 옮겼지만 타입의 성격은 손대지 않았다.

- `GameAnswerDto` — `Game`을 필드로 들고 `getAnswer()`에서 게임 종류로 `switch` 한다. 표현용 데이터가 아니다
- `OnlineMembers` — `excluding(viewerId)` 행동을 갖는다
- `RoomInfo`, `RoomSettingsInfo`, `PlayersInfo`, `MemberInfo` 등 — 정적 팩토리 + 변환 메서드를 가진 read model

### 8. quiz / session 네이밍 (B안)

`Game` 인터페이스는 `quiz/`에 있고 `game`을 다루는 폴더 이름은 `session/`이라 읽는 사람이 헷갈린다. 근본 해결은 리네임이다.

| 지금 | B안 |
| --- | --- |
| `Game` | `Quiz` |
| `AbstractQuizGame` | `AbstractQuiz` |
| `SongGame` | `SongQuiz` |
| `ComputerScienceQuizGame` | `CsQuiz` |
| `ComputerScienceQuiz` | `CsQuestion` |

폴더 재배치 diff에 리네임까지 얹으면 리뷰가 어려워져 미뤘다.

### 9. `RoomMember` / `RoomPresence` 위치

성격은 웹소켓 세션 캐시인데 `GameRoomService.findAllRooms()`가 `roomPresence.countConnectedIn()`을 부르는 바람에 `domain/room`에 남았다. 그 호출을 걷어내면 controller로 내릴 수 있다.

---

## 인프라

### 10. `GameTimer`가 스프링 추상화를 쓰지 않는다

`ScheduledExecutorService`를 직접 든다. 같은 자리에서 `RoomConnectionRegistry`와 `WebSocketConfig`는 `TaskScheduler`를 쓴다.

### 11. Redis 도입 시점

**스크랩 큐 때문이 아니다.** `song_scrape_request` 테이블이 이미 내구성 있는 큐고, `BLPOP`으로 바꾸면 워커 크래시 시 요청이 증발해 오히려 보장이 약해진다.

진짜 트리거는 **다중 인스턴스**다. 지금 아래 셋은 전부 단일 JVM 메모리에 있어서 인스턴스를 2대로 늘리는 순간 깨진다.

- `LockContext` — `ReentrantLock` 맵
- `SseService` — 인메모리 연결 맵
- `GameRoomManager` / `GameSessionManager` — 인메모리 게임 상태

설계는 `docs/design/20260808-backend-scalability-design.md`에 있다.
