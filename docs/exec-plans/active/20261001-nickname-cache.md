# 닉네임을 memberId 로 바꾸고 캐시로 해석한다

작성일: 2026-10-01
상태: 구현 완료, 커밋 대기
브랜치: `fix/nickname-reflects-without-relogin`

## 1. 문제

닉네임을 바꿔도 **로그아웃 전까지** 채팅과 대기실 목록에 옛 이름이 남는다. DB 와 `/api/auth/me`
는 이미 새 이름이라 화면마다 이름이 엇갈린다.

원인은 닉네임을 **문자열로 복사해 두는 스냅샷**이 두 군데 있기 때문이다.

| # | 스냅샷 | 박히는 시점 | 풀리는 시점 |
|---|---|---|---|
| 1 | STOMP 프린시펄 (`ChatController:36` 의 `user.getNickName()`) | WebSocket 핸드셰이크 | 소켓이 끊길 때 = 로그아웃 |
| 2 | `GamePlayer.nickname` (`GameController:144`) | 방 입장·생성 | 방을 나갔다 들어올 때 |

`StompProvider` 의 `useEffect` 는 의존성이 `isAuthenticated` 하나뿐이라(`StompContext.tsx:160`)
닉네임 변경으로는 재연결되지 않는다. 그래서 1번이 로그아웃까지 버틴다.

곁가지로 `App.tsx:111-115` 의 이펙트가 닉네임 변경 직후 `enterLobby` 를 불러 **대기실·게임 중이면
로비로 튕긴다.**

## 2. 방향

**닉네임을 들고 다니지 않는다. memberId 만 들고, 내보낼 때 캐시에서 해석한다.**

- 캐시는 Spring Cache 로 추상화한다. 키는 `memberId`.
- 지금은 로컬 `ConcurrentMapCacheManager`, 3단계 스케일아웃에서 `RedisCacheManager` 로 교체한다
  (`20260928-scale-out-multi-instance.md` 3단계와 같은 결).
- 닉네임 변경은 **write-through** — DB 를 쓰고 같은 흐름에서 캐시도 갱신한다.
- 온라인 사용자 목록도 같은 캐시를 탄다. 로비에서 이미 쓰는 조회라 캐시가 가장 더운 곳이다.

응답 JSON 의 모양은 바꾸지 않는다(`nickname` 필드 유지). **서버가 조립해서 내려주므로 프런트는
채팅·대기실 쪽 변경이 없다.**

### 새로 둘 것 — `domain/member/MemberProfiles`

캐시 값은 문자열이 아니라 레코드다. 역할 뱃지·프로필 이미지가 붙어도 **필드만 늘리면 되고
무효화 구조는 그대로**다.

```java
public record MemberProfile(Long memberId, String nickname, Role role) {}
```

```java
MemberProfile of(Long memberId);                        // @Cacheable(key = memberId)
Map<Long, MemberProfile> allOf(Collection<Long> ids);   // 멀티 get + 미스만 배치 적재
MemberProfile refresh(Long memberId, Member member);    // @CachePut — write-through
void forget(Long memberId);                             // @CacheEvict — 탈퇴 대비
```

`allOf` 는 `@Cacheable` 로 못 쓴다(아래 3.1). `CacheManager` 에서 `Cache` 를 꺼내 직접 멀티 get 하고,
**미스난 id 만 모아 `findAllInOrderByNickname` 한 번**으로 채운다.

캐시 매니저는 Caffeine, **TTL 5분**(`spring.cache.caffeine.spec=expireAfterWrite=5m`). 3단계에서
`spring.cache.type=redis` 로 바꾸면 코드는 그대로다.

### 캐시에 넣지 않는 것 — 접속 상태와 방 위치

`status` 와 `currentRoomId` 는 성질이 다르다.

| | 닉네임 · 역할 | status · currentRoomId |
|---|---|---|
| 권위 소스 | **DB** (`member`) | **메모리** (`GameRoomManager.gameRooms`) |
| 조회 비용 | 쿼리 1회 | 맵 순회 — I/O 없음 |
| 무효화 지점 | 1곳 (`updateNickname`) | 입장·퇴장·킥·게임시작·게임종료·연결끊김·유예만료 |

`locationsOfEveryPlayer()` 는 방 맵을 돌며 그 자리에서 계산한다. **아낄 I/O 가 없고**, 넣는 순간
캐시가 또 하나의 상태 저장소가 된다 — 지금 고치려는 버그와 똑같은 함정이다. 상태 공유는
`20260928-scale-out-multi-instance.md` 3단계의 `RedisPresenceStore` · `RedisRoomRegistry` 자리다.

로비 목록은 **프로필(캐시) + 위치(메모리 계산)** 를 조합해 만든다. 지금 구조 그대로다.

## 3. 먼저 합의할 것

### 3.1 단건 캐시 + 목록 조회는 N+1 이 된다 — 결정: `allOf` 는 수동 멀티 get

`OnlineMemberService.findAllOnline()` 은 지금 `findAllByIdInOrderByNicknameAsc` **한 방 쿼리**다.
이걸 `@Cacheable` 단건 조회로 바꾸면 미스 때 접속자 수만큼 쿼리가 나간다.

→ 위처럼 `allOf` 를 수동 멀티 get 으로 두고, 단건 애너테이션은 채팅·대기실처럼 **한 명만 필요한
경로**에만 쓴다. 정렬이 DB `ORDER BY` 에서 사라지므로 **메모리 정렬로 옮긴다.**

### 3.2 인스턴스가 늘면 write-through 가 자기 노드에만 닿는다 — 결정: TTL 5분

로컬 캐시라 다른 인스턴스는 옛 닉네임을 계속 들고 있다. 지금은 1대라 문제가 없지만 스케일아웃
계획이 있다.

→ **로컬 단계에서도 TTL 5분을 둔다.** 최악의 경우에도 5분 뒤에는 맞는다. Redis 로 가면 TTL 은
그대로 두고 캐시만 공유된다. Caffeine 의존성이 하나 늘어난다.

### 3.3 `GamePlayer.nickname` 제거는 파급이 넓다 — 결정: 지운다

`main` 기준 15개 파일이 닉네임을 그 레코드에서 꺼내 쓴다 — `GamePlayerResponse`, `RoomResponse`,
`RoomSettingsResponse`, `RoomStateResponse`, `PlayerScoreResponse`, `GameNotifyService`,
`HangmanQuiz`, `RoomInviteService`, `GameRank`, `PlayerScore`, `GamePlayers`, `GameRoomService`.
테스트는 그보다 많다.

→ 필드를 **지운다.** 남겨두면 다음 사람이 또 박제한다. 컴파일러가 전부 짚어 주므로 한 번에 간다.
대신 커밋을 쪼갠다(5절).

## 4. 하지 않는 것

- WebSocket 재연결로 푸는 방법 — 닉네임 바꿀 때마다 소켓을 끊으면 방이 끊긴다.
- 프런트가 memberId→닉네임을 직접 그리는 방법 — 온라인 목록을 구독하지 않는 화면(게임 중)에서
  이름을 못 그린다. 서버 조립이 안전하다.
- 회원 가입 시 캐시 적재 — 조회 시 적재로 충분하다.

## 5. 작업 순서 (TDD, 커밋 단위)

| # | 커밋 | 테스트 먼저 |
|---|---|---|
| 1 | `chore: [backend] 포트폴리오 스크린샷을 git 추적에서 뺀다` | — (이미 `.gitignore` 에 반영됨) |
| 2 | `feat: [backend] 회원 프로필 캐시를 Spring Cache 로 둔다` | `MemberProfilesTest` — 단건 적재·멀티 get·미스 배치 |
| 3 | `feat: [backend] 닉네임 변경이 캐시를 함께 갱신한다` | `AuthServiceTest` — 변경 후 `of()` 가 새 이름 |
| 4 | `refactor: [backend] 온라인 목록을 프로필 캐시로 읽는다` | `OnlineMemberServiceTest` — 쿼리 1회, 메모리 정렬 |
| 5 | `fix: [backend] 채팅 닉네임을 프린시펄 대신 캐시에서 읽는다` | `ChatControllerTest` — 변경 직후 새 이름으로 나간다 |
| 6 | `refactor: [backend] GamePlayer 에서 닉네임을 걷어낸다` | 방·세션·알림 테스트 수정 |
| 7 | `fix: [frontend] 닉네임을 바꿔도 방에서 튕기지 않는다` | `App` 이펙트 테스트 |

2~3 이 끝나면 **채팅은 5에서, 대기실은 6에서** 고쳐진다.

## 6. 확인 못 한 것

- testcontainers 통합 테스트는 이 PC 에서 돌지 않는다(Docker Engine 29). 4·5·6 의 컨텍스트 로딩은
  **CI 에서 확인한다.**

---

## 7. 구현하며 드러난 것

### 행맨이 `QuizContent` 의 위치 기반 배열에 턴 플레이어 이름을 담고 있었다
도메인 객체라 캐시에 손이 닿지 않는다. **슬롯 3(닉네임)을 빼고 프런트가 `currentTurnMemberId` 와
`players` 로 이름을 찾도록** 바꿨다. 배열 자리가 하나씩 당겨졌다.

| | 전 | 후 |
|---|---|---|
| 3 | 턴 플레이어 닉네임 | isGameOver |
| 4 | isGameOver | isWin |
| 5 | isWin | 턴 플레이어 memberId |
| 6 | 턴 플레이어 memberId | — |

### 행맨이 `PlayerScore` 를 (라벨, 값) 용도로 재활용하고 있었다
`resultRow("성공", 남은기회)` 처럼 memberId 없이 닉네임 자리에 문구를 넣어 결과 화면을 그렸다.
닉네임이 사라지면서 자리가 없어져 `ResultRow(memberId, label, score)` 를 새로 뒀다. 사람이 주인인
줄은 memberId 로 이름을 찾고, 행맨의 고정 문구는 label 을 쓴다.

### 그 밖에 바뀐 동작
- `GameRank` 의 동점자 정렬 기준이 닉네임에서 memberId 로 바뀌었다.
- 온라인 목록 정렬이 DB collation 이 아니라 자바 문자열 비교를 탄다.
- `LeaveResult.nickname` 이 `wasInRoom` 플래그가 됐다. 닉네임의 null 여부로 "방에 있었나"를
  판단하던 코드였다.

## 8. 남은 것

- 테스트컨테이너가 필요한 스위트 19개는 이 PC 에서 돌지 않는다(Docker Engine 29). **CI 에서 확인한다.**
- `useGameLogic.ts` 에 기존 lint 오류(`any` 5건)와 경고가 있다. 이번 변경과 무관해 건드리지 않았다.
