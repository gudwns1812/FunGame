# 커넥션 풀 고갈 해소 계획 (#38)

## 1. 목표 (Goal)

운영 서버에서 `HikariPool-1 - Connection is not available (active=30, waiting=160)` 로 전체 요청이 멈추는 문제를 해소한다. 이슈 #38 의 2번, 3번을 고친다.

## 2. 배경 및 문맥 (Context)

- **2번**: `GameRoomService` 의 `@Transactional` 이 `LockContext` 의 방 락 대기 구간을 감싸고 있어, 락을 기다리는 스레드가 DB 커넥션을 쥔 채로 논다.
- **3번**: 방/접속 상태가 바뀌면 SSE 로 접속자 전원에게 `"REFRESH"` 문자열만 보내고, 각 클라이언트가 다시 HTTP 로 목록을 조회한다. 접속자 N 명이면 이벤트 1건이 요청 N 건이 된다.

## 3. 핵심 변경 파일 (Key Files)

### 백엔드
- `domain/GameRoomService.java`: `createRoom`/`joinRoom`/`leaveRoom` 의 `@Transactional` 제거.
- `support/sse/SseService.java`: 이벤트 수신·페이로드 조립 책임을 걷어내고 전송 수단으로만 남긴다. `broadcast`, `broadcastEach` 공개.
- `controller/sse/LobbyNotifyService.java` (신규): 방·접속 변경 이벤트를 모아 실제 목록을 조회해 SSE 로 push.
- `domain/member/OnlineMemberService.java`: 한 번의 조회 결과를 여러 조회자에게 재사용할 수 있도록 `findAllOnline` 추가.
- `domain/dto/OnlineMembers.java` (신규): 조회자 본인을 제외한 목록을 만드는 타입.

### 프론트엔드
- `hooks/useGameLogic.ts`: `room-update` 페이로드로 방 목록 갱신.
- `hooks/useOnlineMembers.ts`: `presence-update` 페이로드로 접속자 목록 갱신.

## 4. 설계 및 아키텍처 결정 사항 (Design Decisions)

- **트랜잭션 경계는 락 안쪽으로만.** 트랜잭션은 `GameRoomStore` 와 `MemberPresenceService` 에 이미 걸려 있으므로 서비스의 것만 걷어내면 된다. 방 상태와 회원 위치는 서로 다른 애그리거트이고 회원 위치는 기동 시 `clearEveryLocation` 으로 복구되므로, 두 쓰기가 한 트랜잭션이 아니어도 된다.
- **presence 는 조회 한 번, 전송은 연결별로.** 접속자 목록은 조회자 본인을 빼야 해서 사람마다 페이로드가 다르다. DB 는 한 번만 읽고 걸러내기만 메모리에서 한다.
- **SSE 는 전송 수단으로만 둔다.** 무엇을 실어 보낼지는 도메인을 아는 `LobbyNotifyService` 가 정한다. `GameNotifyService` 가 STOMP 로 하는 일과 같은 자리다.
- **구버전 클라이언트 보정.** 배포 전환 중에는 `"REFRESH"` 가 올 수 있으므로, 페이로드를 해석하지 못하면 기존처럼 HTTP 재조회로 넘어간다.

## 5. 테스트 전략 (Testing Strategy)

TDD 로 진행한다.

- `GameRoomServiceTransactionBoundaryTest` (신규): 방 락을 잡는 지점에서 트랜잭션이 열려 있지 않은지 검증한다. 실제 프록시가 필요하므로 최소 스프링 컨텍스트를 띄운다.
- `LobbyNotifyServiceTest` (신규): 이벤트를 모아 한 번만 보내는지, 실제 목록을 실어 보내는지, 접속자 조회를 한 번만 하는지 검증한다.
- `SseServiceTest`: 브로드캐스트 관련 검증을 전송 관점으로 옮긴다.
- `OnlineMemberServiceTest` (신규): 본인 제외 규칙을 검증한다.
- `useGameLogicSse.test.ts`, `useOnlineMembers.test.ts`: 페이로드를 받으면 재조회 없이 갱신하는지, 해석 불가 페이로드면 재조회로 보정하는지 검증한다.

도커가 없는 환경이라 testcontainers 통합 테스트는 실행하지 않는다.
