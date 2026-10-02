# iteration-3 채점 요약 (one-home-per-value, 작은 모델 / Haiku)

## 1. 통과율 표

| eval | with_skill | without_skill | 차이 |
|---|---|---|---|
| eval-3 notification-actor-name | 0.75 (3/4) | 0.00 (0/4) | **+0.75** |
| eval-4 ranking-cache-payload | 0.75 (3/4) | 0.00 (0/4) | **+0.75** |
| eval-5 event-payload-mixed | 1.00 (4/4) | 1.00 (4/4) | 0 |
| **평균** | **0.833** | **0.333** | **+0.500** |

참고(timing.json): with 가 일관되게 토큰·시간을 더 쓴다. 43.3k/42.3k/44.0k 대 40.7k/37.4k/37.0k, 49.9s/50.6s/60.1s 대 54.0s/38.2s/36.8s. 즉 스킬이 추가 사고 비용을 유발하고 있고, eval-3·4 에서는 그 비용이 결과로 환수됐다.

참고(iteration-2, 큰 모델): 0.917 대 0.917 로 차이가 **0** 이었다. 같은 단언·같은 프롬프트인데 모델 체급만 낮추자 격차가 열렸다.

---

## 2. 두 조건의 판정이 갈린 단언 (좌우 대조)

### eval-3 단언 1 — "Notification 엔티티에 행위자의 닉네임·프로필 썸네일 같은 표시용 값을 컬럼으로 두지 않는다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| `@Column(name = "actor_id", nullable = false) private Long actorId; // ★ 행동한 사람 ID만 저장. 닉네임/프로필은 저장 X` <br><br> "✅ **엔티티에는 actorId만**: 닉네임/프로필 복사 금지" | `private String actorName; // Actor의 이름 (역정규화)` <br> `private String actorProfileImageUrl; // Actor의 프로필 이미지 URL (역정규화)` <br><br> 설계 개요 첫 줄부터 "**Actor 정보 역정규화**: 알림 조회 시 Member 테이블 조인 없이 즉시 이름/이미지 표시" 를 원칙으로 선언. 마이그레이션 SQL 에도 `actor_name VARCHAR(100) NOT NULL` 이 박혀 있다 |

정확히 반대 방향의 설계를 내놓았다. 이 단언 하나가 iteration-3 에서 가장 큰 차이다.

### eval-3 단언 2 — "목록 20건 조회 시 행위자 정보를 한 번에 모아 해석하고, 건당 조회를 반복하지 않는 방법을 코드로 보인다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| `List<Long> actorIds = notificationPage.getContent().stream().map(Notification::getActorId).distinct().toList();` <br> `Map<Long, Member> memberMap = memberRepository.findByIdIn(actorIds).stream().collect(Collectors.toMap(Member::getId, Function.identity()));` <br> "★ 한 번에 모든 Member 조회 (N+1 방지)" | 일괄 조회 코드가 **아예 없다.** `NotificationActorResponse.from(notification)` 이 엔티티에 복사해 둔 `actorName` 을 그대로 읽는다. N+1 은 "역정규화 … Member 조인 제거" 로 회피했으므로, 단언이 요구한 "모아서 해석하는 코드"를 보일 기회 자체가 사라졌다 |

### eval-3 단언 3 — "알림에 담긴 사람 이름이 '그때 값'인지 '지금 값'인지를 명시적으로 판단해 밝힌다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| "행동 당시 닉네임은 정해졌지만, 사용자가 나중에 닉네임을 바꾸면 '목록에 보이는 사람 이름이 달라졌다'는 버그가 생깁니다. 대신 **memberId만 저장하고, 응답을 조립할 때 현재 정보를 조회해서 담습니다.**" <br> `actor.getNickname(), // ★ 현재 닉네임` <br> "✅ **Member 변경 시 자동 반영**: 사용자가 닉네임 바꾸면 다음 조회부터 바뀐 이름이 보임" | 시점 판단 문장이 **전무**. 가장 근접한 언급이 성능 표의 "역정규화 / actorName, actorProfileImageUrl을 Notification에 저장하여 Member 조인 제거" 로, 값이 늙는다는 사실을 다루지 않는다 |

### eval-3 단언 4 — "멘션 본문 미리보기에 닉네임 문자열을 그대로 저장하지 말고 식별자·토큰으로 저장하라고 한다" (양쪽 실패, 차이 없음)

| with_skill (실패) | without_skill (실패) |
|---|---|
| `private String contentPreview; // "그때" 본문 미리보기. 시점을 이름에 드러냄.` <br> 응답 예시: `"contentPreview": "오늘 그 영화 봤어? 정말 재미있더라 @박영희가 꼭 봐야..."` — 생 닉네임 그대로 | `"좋은 의견입니다 @김철수");  // 댓글 본문 미리보기` <br> 응답 예시: `"contentPreview": "좋은 의견입니다 @김철수"` — 동일하게 생 닉네임 |

iteration-2 와 **완전히 같은 실패 양상**이다. 본문 '안의' 멘션 토큰화는 어느 체급에서도, 스킬 유무와 무관하게 아무도 건드리지 않았다. 단언 자체가 프롬프트에서 유도되지 않는 숨은 요구임을 재확인한다.

### eval-4 단언 1 — "캐시 값에 nickname 을 넣으면 TTL 동안 옛 이름이 서빙된다고 지적한다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| "T1: 사용자가 닉네임을 '예진' → '예진_v2'로 변경 / **T2~T60: 클라이언트는 여전히 '예진'을 본다 (캐시가 살아있음)** / T61: 캐시 만료 후 새로 조회하면 '예진_v2'로 보임" <br> "이는 **'왜 새로고침해야 닉네임이 바뀌나'** 는 오래된 값 문제를 만든다" | 지적 **없음**. 오히려 최종 권장 설계가 `record RankingEntry(int rank, Long memberId, String nickname, int score) {}` 로 nickname 을 그대로 유지하고, 캐시 내용에 대한 유일한 평가가 "JSON 크기 추정: 100명 × 약 200 바이트 = 20KB 정도 (괜찮음)" 이다 |

### eval-4 단언 2 — "점수·순위와 표시 이름은 수명이 다른 값이라는 점을 구분해 설명한다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| "Member 테이블에는 **Member가 소유한 값**으로 nickname이 산다 / Redis 캐시에는 그 값의 **사본**이 들어 있다 / **원본과 사본은 따로 늙는다**" <br> "랭킹 캐시는 집계 결과(순위, 점수) 보존에만 쓴다 / 닉네임 캐시는 개인 정보 조회 성능에만 쓴다 / 닉네임이 바뀌면 그 캐시만 무효화하면 된다" (닉네임 캐시 TTL 은 "충분히 길거나 무제한") | 값의 수명 구분이 **없다**. 캐시를 "집계 결과 덩어리" 하나로만 보고 TTL·백업·원자적 교체·압축 같은 운영 주제로만 다룬다 |

### eval-4 단언 3 — "닉네임을 캐시 값에서 빼거나, 넣더라도 무효화·갱신 경로를 함께 제시한다"

| with_skill (통과) | without_skill (실패) |
|---|---|
| `record RankingEntry(int rank, Long memberId, int score) {}` <br> "닉네임을 **제거한다**. 식별자인 `memberId`만 가진다" <br> 서빙 시 `Map<Long, String> nicknames = memberService.getNicknamesByIds(memberIds);` 로 경계에서 해석 | 닉네임 무효화·갱신 경로 없음. 무효화 논의는 배치 실패 백업(`backup:ranking:weekly:2026-W39`)과 `redisTemplate.rename(tempKey, key)` 같은 교체 원자성에만 머문다 |

### eval-4 단언 4 — "집계는 주 1회인데 TTL 이 1시간이라 한 시간마다 무거운 집계가 다시 돈다는 모순을 짚는다" (양쪽 실패, 차이 없음)

| with_skill (실패) | without_skill (실패) |
|---|---|
| TTL 과 배치 주기의 불일치, 재집계 비용에 대한 언급이 **전혀 없다.** TTL 은 오로지 "T2~T60: 클라이언트는 여전히 '예진'을 본다" 처럼 *옛 닉네임이 서빙되는 창* 으로만 쓰인다 | 오히려 반대로 칭찬한다 — "✅ 무거운 집계 쿼리를 1시간에 한 번만 실행 → 데이터베이스 부하 급감", "✅ 1시간 TTL은 실시간성과 캐시 효율의 균형이 적절". TTL 재검토 항목이 있긴 하나 "월요일 새벽 배치 후 1시간 TTL이면, 월요일 오전과 나머지 6.5일의 **캐시 유지율이 다름**" 이라는 유지율 관점이어서, 재집계가 주당 168번 돈다는 모순을 짚지 못한다 |

**주의**: 이 단언은 iteration-2 에서 양쪽 모두 "주당 168번" 까지 계산해 통과했던 항목이다. 체급을 낮추자 스킬 유무와 무관하게 둘 다 놓쳤다 — 스킬이 메우지 못한 영역이다. 특히 with_skill 은 "값의 집" 쪽으로 시야가 좁아져 TTL 의 비용 측면을 아예 보지 않았다는 점이 눈에 띈다.

### eval-5 (4개 단언 전부) — 양쪽 모두 통과, 차이 없음

| 단언 | with_skill | without_skill |
|---|---|---|
| 사실/표시값 분리 | "유지해야 할 필드 / `totalAmount` / **거래 당시의 거래액 (스냅샷)**" vs "`memberName` … 스냅샷으로 남길 명확한 비즈니스 요구사항이 없다면 복사하지 말 것" | "**불변값만** - 금액, 상태, 타임스탬프처럼 변하지 않는 값은 포함 가능" vs "이름, 이메일 등 변경될 수 있는 정보는 각 서비스의 DB에서만 관리" |
| 사건의 사실 인정 | `status`·`totalAmount`·`changedAt` 모두 유지 표에 등재 | "status: 상태 변화가 이벤트의 핵심이다. 절대 필요", "totalAmount: … 이벤트 발행 시점의 확정 금액을 보존", "changedAt: 감사 추적(audit log)과 chronological 순서 보장에 필수" |
| 이름은 빼고 식별자로 해석 | "컨슈머의 책임: 알림 서비스는 `memberId`를 들고, 푸시 메시지 조립 시 현재 닉네임으로 조회" | "알림 서비스: 이미 memberId로 자신의 DB에서 회원 정보를 조회한다. 중복이다" |
| memberEmail 제거 | "**제거**: `memberName`, `memberEmail`, `sellerName`" | "2. `memberEmail` (특히) ❌ 빼기 (메시지 템플릿이 필요하면 별도로)" |

eval-5 는 스케치 자체가 `memberName`/`memberEmail`/`sellerName` 을 노골적으로 박아둔 유도형 문제라, 작은 모델도 스킬 없이 만점을 낸다.

---

## 3. 과적용 점검 — 스킬이 틀린 답을 만든 적이 있는가?

**없다. 세 with_skill 런 모두, 정당한 스냅샷을 빼라고 말하지 않았다.**

### (a) 주문 시점 거래액 (eval-5)

with_skill 은 `totalAmount` 를 **명시적으로 유지**시켰다.

> "`totalAmount` / **거래 당시의 거래액 (스냅샷)** — 이후 환불/수정이 별도로 처리되므로 유지 필요"
>
> "**`totalAmount` 만 특례** — 거래액은 그 시점에 정해진 금액 / 이후 변경되면 환불·부분취소로 별도 처리 / **따라서 스냅샷이 맞다**"

개선 payload 에도 `"totalAmountAtOrder": 48000` 으로 살아 있다. `changedAt`, `status` 도 "이벤트의 핵심", "정산 시 필요" 로 유지했다. 단언 2 가 요구한 "전부 식별자만 담으라고 하면 실패" 함정에 걸리지 않았다.

### (b) 멘션 본문 미리보기 (eval-3)

with_skill 은 `contentPreview` 를 **엔티티에 남겼다.**

> `@Column(name = "content_preview", columnDefinition = "VARCHAR(500)") private String contentPreview;  // "그때" 본문 미리보기. 시점을 이름에 드러냄.`
>
> "✅ **MentionNotification.contentPreview는 스냅샷**: 멘션 당시 본문이므로 변경 추적 불필요"

즉 "본문 미리보기도 복사본이니 빼라" 는 과잉 적용을 스스로 차단했다. (다만 그 미리보기 **안의 닉네임** 을 토큰화하라는 더 깊은 요구는 놓쳤다 — 이건 과적용이 아니라 미적용 쪽 실패다.)

### (c) 스냅샷이 필요할 때의 처방도 함께 제시

with_skill 은 세 런 모두 "정말 스냅샷이 필요하면 이름에 시점을 박아라" 는 탈출구를 같이 제시한다. eval-4: `nicknameAtSnapshot,  // <- 시점을 이름에 명시`. eval-5: `memberNameAtOrder`, `sellerNameAtOrder` 를 제시하고 "역사적 기록이 정말 필요하다면 … 애초부터 스냅샷으로 설계하되, 필드명에 시점을 박는다". 규칙을 맹목적으로 밀어붙이는 대신 **예외를 설계로 흡수**하는 형태다.

**결론: 이번 iteration 에서 과적용(over-application)으로 인한 오답은 0건.** 스킬이 올린 통과율이 "무조건 빼라"는 편향의 부작용으로 상쇄되지 않았다.

---

## 4. 판정 — 작은 모델에서는 스킬이 결과를 바꾼다

iteration-2(큰 모델)에서 0.917 대 0.917 로 **변별력이 0** 이었던 동일한 단언 세트가, 체급을 Haiku 로 낮추자 0.833 대 0.333 으로 갈렸다 — 12개 단언 중 6개의 판정이 뒤집혔고, 그 6개가 전부 **스킬이 다루는 바로 그 판단**(표시용 값을 레코드에 복사하지 말 것, 캐시 사본이 따로 늙는다는 인식, 경계에서 일괄 해석)이다. 특히 eval-3 without_skill 은 단순히 답을 놓친 게 아니라 "Actor 정보 역정규화"를 **설계 원칙으로 내세워 정반대 방향**으로 갔고, eval-4 without_skill 은 닉네임이 캐시에 들어 있다는 사실을 끝까지 문제로 인식조차 못 했다. 반면 스케치가 안티패턴을 노골적으로 박아둔 eval-5 는 양쪽 만점으로, 작은 모델도 **문제가 답을 유도하면** 스킬 없이 해낸다. 종합하면 이 스킬의 가치는 "모델이 모르는 것을 가르치는 것"이 아니라 **문제가 눈에 띄게 만들어주지 않을 때 그 판단을 반드시 수행하게 만드는 규율** 에 있고, 그 규율의 한계도 분명하다 — 값의 소유권과 무관한 축(eval-4 의 TTL 대 배치 주기 비용 모순)은 스킬이 오히려 시야를 좁혀 양쪽 모두 놓쳤다. 추가 비용은 런당 2~7k 토큰, 최대 +23초로 실용 범위다. 요약하면: **이 스킬은 큰 모델에는 중복이지만 작은 모델에는 유효하며, 측정 가능한 부작용은 관찰되지 않았다.**
