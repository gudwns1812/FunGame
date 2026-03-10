# 할리갈리(HaliGali) 게임 로직 상세 가이드

본 문서는 할리갈리 게임의 백엔드 구현 로직을 게임 시작, 진행(액션), 종료의 생명주기에 따라 상세히 설명합니다.

---

## 1. 게임 준비 및 시작 (Setup & Start)

### 1-1. 카드 덱 생성 및 분배
- **총 카드 수**: 56장
- **카드 구성**: 4가지 과일(딸기, 바나나, 라임, 포도)별로 다음과 같이 구성됩니다.
  - 1개: 5장
  - 2개: 3장
  - 3개: 3장
  - 4개: 2장
  - 5개: 1장
  - (각 과일당 14장 × 4과일 = 56장)
- **분배**: 모든 플레이어에게 균등하게 카드를 분배합니다. (예: 4명일 경우 인당 14장)
- **상태 초기화**:
  - `playerDecks`: 각 플레이어가 가진 뒤집히지 않은 카드 더미 (Deque).
  - `openCards`: 각 플레이어가 바닥에 뒤집어 놓은 카드 더미 (Deque).

### 1-2. 게임 시작 흐름
1. `BoardGameService.startGame` 호출.
2. `GameSession` 생성 및 플레이어별 덱 셔플/분배.
3. `GAME_START` 이벤트 발행.
4. 즉시 현재 상태(첫 번째 턴 정보)를 포함한 `HALIGALI_ACTION` 이벤트 발행.

---

## 2. 게임 진행 (Game Flow)

### 2-1. 라운드 기반 턴 관리
- **라운드(Round)**: 시스템 내부적으로 `currentRound`를 사용하여 현재 턴을 관리합니다.
- **턴 결정**: `(currentRound - 1) % players.size()` 인덱스에 해당하는 플레이어가 현재 턴의 주인공입니다.
- **자동 건너뛰기**: 카드가 0장인 플레이어는 `nextRound()` 시 자동으로 스킵되어 다음 살아있는 플레이어에게 턴이 넘어갑니다.

### 2-2. 액션 1: 카드 뒤집기 (`FLIP_CARD`)
1. **권한 확인**: 현재 턴인 플레이어만 수행 가능합니다.
2. **카드 이동**: 자신의 `playerDecks`에서 카드 1장을 뽑아 자신의 `openCards` 맨 위에 놓습니다.
3. **턴 종료**: `nextRound()`를 호출하여 다음 플레이어에게 턴을 넘깁니다.

### 2-3. 액션 2: 종 울리기 (`PRESS_BELL`)
- **권한 확인**: 턴과 관계없이 **누구나** 언제든지 종을 울릴 수 있습니다.
- **판정 로직**: 모든 플레이어의 `openCards` 맨 위에 있는 카드들의 과일별 합계를 계산합니다.
  - **성공**: 어떤 과일이든 합계가 **정확히 5개**인 경우.
  - **실패**: 합계가 5개인 과일이 하나도 없는 경우.

---

## 3. 보상 및 패널티 (Reward & Penalty)

### 3-1. 종 울리기 성공 (보상)
- 종을 울린 플레이어(Winner)가 바닥에 깔린 **모든 플레이어의 `openCards`**를 가져갑니다.
- 수거한 카드들은 Winner의 `playerDecks` 맨 아래(Last)로 추가됩니다.
- 바닥이 비워지며 게임이 계속됩니다.

### 3-2. 종 울리기 실패 (패널티)
- 종을 잘못 울린 플레이어(Loser)는 자신의 `playerDecks`에서 카드를 꺼내 **다른 모든 살아있는 플레이어에게 1장씩** 나눠줍니다.
- 받은 카드들은 각 플레이어의 `playerDecks` 맨 아래로 추가됩니다.
- 만약 Loser의 카드가 부족하면 가진 만큼만 나눠줍니다.

---

## 4. 게임 종료 (Game End)

### 4-1. 플레이어 탈락
- 자신의 `playerDecks`와 `openCards`가 모두 비어있는 플레이어는 게임에서 제외(스킵)됩니다.

### 4-2. 최종 승리 및 종료
- **종료 조건**: 살아있는 플레이어가 **1명** 이하가 되면 게임이 즉시 종료됩니다.
- **결과 산출**: 각 플레이어가 보유한 총 카드 수(`playerDecks` + `openCards`)를 기준으로 순위를 매깁니다.
- **종료 흐름**:
  1. `GAME_RESULT` 이벤트를 통해 최종 순위 전파.
  2. 5초 대기 후 `GAME_END` 이벤트를 발행하여 방을 대기실 상태로 전환.

---

## 5. 데이터 통신 구조 (Communication)

1. **Client**: `POST /game/rooms/{roomId}/action` (JSON: `playerName`, `type: FLIP_CARD`)
2. **Controller**: `GameController` -> `GameService(Router)` -> `BoardGameService`
3. **Service**: `BoardGameService`가 `HaliGaliGame` 도메인 액션 호출.
4. **Domain**: `HaliGaliGame` 내부 상태 변경 및 `ActionResult` 반환.
5. **Event**: `BoardGameService`가 `HaliGaliActionEvent` 발행.
6. **Notify**: `GameNotifyService`가 웹소켓(`HALIGALI_ACTION`)으로 현재 바닥 상태와 턴 정보를 브로드캐스트.
