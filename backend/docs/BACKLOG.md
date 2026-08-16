# BACKLOG

리팩토링 도중 확인했지만 범위 때문에 미뤄둔 것들. 각 항목은 "무엇을, 왜 지금 안 했는지, 어디를 보면 되는지"만 적는다.

---

## 구조

### 1. client 모듈 의존 (결론: 현행 유지)

`core-api → clients:*`는 `ARCHITECTURE.md`가 의도한 구조다. 이걸 끊으려면 `clients`가 `core-api`를 의존해야 하는데, 그러면 조립을 맡을 제3 모듈이 필요하다(Gradle이 순환을 거부한다). 그 모듈은 `SongquizApplication`을 가져가므로 **`@SpringBootTest` 통합테스트 15개가 부팅 지점을 잃고**, Dockerfile 3줄이 바뀐다. 비용 대비 이득이 없어 원래대로 뒀다.

---

## 남은 것

### 2. `GameRoomReader`가 방을 읽을 때마다 닉네임을 손 조인한다

방은 memberId만 영속하므로 `findNicknames()`로 회원을 따로 읽어 붙인다. 이 자체는 정규화된 구조라 그대로 두는 게 맞지만, 회원 행이 사라진 참가자를 `nickname != null`로 조용히 걸러내는 건 의도한 동작인지 확인이 필요하다.

### 3. `session` 폴더 이름

`quiz/`의 타입은 전부 `Quiz*`로 정리했지만 `session/`에는 `GameSession`, `GameService`, `GameRank` 처럼 `Game*`이 남아 있다. 세션은 방과 퀴즈를 잇는 개념이라 `Game`이 틀린 말은 아니어서 손대지 않았다. 폴더까지 정리하려면 여기부터 이름을 정해야 한다.

---

## 인프라

### 4. Redis 도입 시점

**스크랩 큐 때문이 아니다.** `song_scrape_request` 테이블이 이미 내구성 있는 큐고, `BLPOP`으로 바꾸면 워커 크래시 시 요청이 증발해 오히려 보장이 약해진다.

진짜 트리거는 **다중 인스턴스**다. 지금 아래 셋은 전부 단일 JVM 메모리에 있어서 인스턴스를 2대로 늘리는 순간 깨진다.

- `LockContext` — `ReentrantLock` 맵
- `SseService` — 인메모리 연결 맵
- `GameRoomManager` / `GameSessionManager` — 인메모리 게임 상태

설계는 `docs/design/20260808-backend-scalability-design.md`에 있다.
