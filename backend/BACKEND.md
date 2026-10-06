# 백엔드 작업 지침 (BACKEND.md)

모듈·계층·패키지 구조는 [ARCHITECTURE.md](ARCHITECTURE.md) 가 기준이다. 이 문서에는 구조 밖의 작업 규칙을 적는다.

## 기술 스택

- **언어:** Java 21
- **프레임워크:** Spring Boot, Spring WebSocket (STOMP)
- **빌드 도구:** Gradle
- **데이터베이스:** MySQL 8.0 (운영), H2 (`local` 프로파일), Redis

## 작업 지침

1. **TDD 준수:** 모든 기능 구현 전 `src/test`에 테스트 코드를 먼저 작성합니다.
2. **도메인 객체가 규칙을 가진다:** 서비스에 로직을 몰아넣지 않고, 도메인 객체가 스스로 상태를 바꾸고 규칙을 검증합니다.
3. **상태 변화는 이벤트로 알린다:** 도메인 상태가 바뀌면 `ApplicationEventPublisher` 로 이벤트를 발행하고, WebSocket 전송은 `GameNotifier` 같은 리스너가 맡습니다.
4. **예외 처리:** 비즈니스 에러는 `ErrorType` 에 정의하고 런타임 예외 `CoreException` 으로 던집니다. `ApiControllerAdvice` 가 공통 응답 포맷으로 바꿉니다.
5. **의존성 주입:** 생성자 주입만 씁니다. Lombok 의 `@Data`, `@AllArgsConstructor` 는 쓰지 않습니다.
6. **근거 없는 일반화 금지** : 확장은 지표가 한계를 보일 때 합니다. 목표 아키텍처(방과 판을 Redis 원본으로 + Redis Streams 팬아웃)와 단계별 진입 조건은 [docs/exec-plans/active/20260928-scale-out-multi-instance.md](../docs/exec-plans/active/20260928-scale-out-multi-instance.md) 에 있습니다. 그 문서의 단계에 없는 분산 장치(외부 STOMP 브로커, 이벤트 소싱, 서비스 분해, Kubernetes)는 도입하지 않습니다.
7. **인스턴스는 여러 대가 될 수 있다** : 서버 1대를 전제한 코드는 더 이상 쓰지 않습니다. 새로 만드는 상태는 **인스턴스 로컬이어도 되는지**를 먼저 판단하고, 아니라면 공유 저장소(DB·Redis) 뒤에 둡니다. `@Scheduled` 작업에는 **로컬 대상인지 전역 대상인지**를 주석으로 남깁니다. 기존 인메모리 상태를 지금 당장 전부 걷어내라는 뜻은 아닙니다 — 계획 문서의 단계를 따릅니다.
8. **Redis Lua 에는 저장 연산만 둔다** : Lua 는 "revision 이 그대로일 때만 쓴다", "시각이 된 것을 미뤄 두며 가져간다", "만료된 것을 꺼낸다" 처럼 규칙 없는 원자 연산만 맡습니다. 정원 · 유예 · 정답 같은 **판단은 자바 도메인 객체**에 두고, 읽고 → 자바로 적용하고 → 비교 후 쓰기로 바꿉니다(`RoomStore`). 판단과 원자성이 함께 필요한데 비교 후 쓰기로 묶기 어려우면 Lua 대신 자바에서 `WATCH` + `MULTI` 로 하고 충돌하면 다시 시도합니다(`MemberPresenceDao`). 규칙이 Lua 로 새면 DB 프로시저처럼 앱 코드만 읽어서는 동작이 안 보이고 단위 테스트로 검증할 수 없습니다. Lua 는 `storage:redis-core` 의 DAO 안에만 둡니다. 락(`SET NX` + 해제)으로 같은 일을 하지 않습니다 — 왕복이 늘고 락을 쥔 인스턴스가 죽으면 그 키가 TTL 동안 막힙니다.

## 테스트

- **단위 테스트:** 도메인 로직은 스프링 없이 JUnit5 와 AssertJ 로 검증합니다. 가짜가 필요하면 목보다 스텁을 먼저 씁니다. 외부 라이브러리 때문에 스텁이 지저분해질 때만 목을 허용합니다.
- **통합·인수 테스트:** 스프링 컨텍스트는 하나만 띄웁니다. 빈 바꿔치기는 `ApiIntegrationTest` 한 곳에 모읍니다. 클래스마다 설정이 갈리면 컨텍스트를 다시 띄워 CI 가 느려집니다.
- 테스트 메서드 이름은 한글을 허용합니다.

## API 문서

1. **RestDocs 스니펫:** 컨트롤러를 새로 만들면 RestDocs 테스트(`*DocsTest`)를 함께 씁니다. 성공뿐 아니라 주요 예외(400, 404 등) 스니펫도 남깁니다. 결과물은 `bootJar` 가 `static/docs` 로 넣습니다. 상세는 [ARCHITECTURE.md](ARCHITECTURE.md#api-문서).
2. **`api/*.md`:** 루트 [api/](../api/) 는 손으로 관리하는 명세다. 빌드 산출물이 그리로 흘러가지 않으므로, API·이벤트 페이로드를 바꾸면 같은 PR 에서 직접 고칩니다.

## 함께 보는 문서

- [docs/BACKLOG.md](docs/BACKLOG.md) — 범위 때문에 미뤄 둔 일
- [docs/LEGACY.md](docs/LEGACY.md) — 개선이 필요한 기존 구조
- [ARCHITECTURE_MIGRATION.md](ARCHITECTURE_MIGRATION.md) — 목표 구조로 가는 단계별 기록
