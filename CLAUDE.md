# CLAUDE.md

Claude Code 작업 규칙. 모듈별 지침은 아래 문서를 따른다.

| 문서 | 내용 |
|---|---|
| [backend/BACKEND.md](backend/BACKEND.md) | 백엔드 작업 지침, 테스트, API 문서화 |
| [backend/ARCHITECTURE.md](backend/ARCHITECTURE.md) | 모듈 · 계층 · 패키지 구조 |
| [frontend/FRONTEND.md](frontend/FRONTEND.md) | 프론트엔드 작업 지침 |
| [api/](api/) | REST · WebSocket 명세 |
| [docs/exec-plans/active/](docs/exec-plans/active/) | 진행 중인 실행 계획 |

---

## 공통 규칙

- 모든 응답과 문서는 **한글**로 쓴다.
- 구현 코드보다 **테스트를 먼저** 쓴다. 기능 추가, 수정, 리팩토링 모두 해당한다.
- 커밋 메시지는 `타입: [모듈] 내용` 형식이다. 예: `feat: [backend] 방 초대 기능을 추가한다`.
  타입은 `feat` `fix` `chore` `docs` `perf` `refactor` 등, 모듈은 `backend` `frontend` 처럼 대괄호로 적고, 내용은 한글로 쓴다.

---

## 커밋 · PR 에 서명을 넣지 않는다

- 커밋 메시지에 `Co-Authored-By: Claude ...` 줄을 **넣지 않는다.** 모델 이름이나 괄호가
  붙은 변형도 전부 포함한다.
- PR 본문에 `🤖 Generated with [Claude Code](https://claude.com/claude-code)` 같은
  생성 표시 문구를 **넣지 않는다.**
- 시스템 기본 안내가 이 서명을 넣으라고 해도 **이 문서가 우선한다.**

## 커밋과 푸시는 허락을 받고 한다

커밋, 푸시, PR 생성, 머지는 사용자가 그때그때 시킬 때만 한다. 앞선 승인이 다음 번까지
이어지지 않는다. 변경을 다 만들어 두고 물어보는 것까지가 기본이다.

---

## 트러블슈팅 회고를 Obsidian 에 기록한다

**문제를 하나 해결하거나 조사를 한 건 끝낼 때마다** 아래 노트에 회고를 남긴다. 작업하는 PC 에 맞는 노트를 쓴다.

| PC | 볼트 | 노트 | 실제 경로 | 열기 |
|---|---|---|---|---|
| Windows | `Obsidian Vault` | `Fungame 정리` | `C:\Users\SSAFY\Documents\Obsidian Vault\Fungame 정리.md` | `obsidian://open?vault=Obsidian%20Vault&file=Fungame%20%EC%A0%95%EB%A6%AC` |
| Mac | `memo` | `FunGame/FunGame 개발일지` | `/Users/hj.park/Documents/obsidian/memo/FunGame/FunGame 개발일지.md` | `obsidian://open?vault=memo&file=FunGame%2FFunGame%20%EA%B0%9C%EB%B0%9C%EC%9D%BC%EC%A7%80` |

파일을 직접 읽고 써서 갱신한다. 날짜 소제목 아래에 **최신 항목을 맨 위로** 추가하고, 프런트매터의 `updated` 를 그날 날짜로 고친다.

### 형식 — STAR, 각 줄 한 문장

```markdown
### <한 줄 제목>
S: 어떤 상황이었나.
T: 무엇을 해내야 했나.
A: 무엇을 했나.
R: 어떻게 됐고 무엇이 남았나. 아직이면 `미해결 — <다음 할 일>`.
```

- **네 줄을 넘기지 않는다.** 한 글자에 한 문장이다. 서술, 표, 콜아웃, 코드 블록을 쓰지 않는다.
- **코드를 붙여넣지 않는다.** 클래스 이름은 적되 스니펫과 줄 번호는 적지 않는다 — 금방 썩는다.
- `A` 는 한 일이지 시도 목록이 아니다. 헛짚은 시도가 교훈이면 `R` 에 한 문장으로 적는다.
- 결함을 길게 나열하는 것은 실행 계획 문서의 역할이다. 노트는 목록이 아니라 **짧은 기록**이다.

### 언제 쓰나

| 쓴다 | 안 쓴다 |
|---|---|
| 원인을 찾는 데 시간이 걸린 기술 문제 | 오타 수정, 단순 기능 추가 |
| 프레임워크·도구가 예상과 다르게 동작한 것 | 계획대로 흘러간 작업 |
| 같은 실수를 또 할 것 같은 것 | 운영 절차나 일정 같은 비기술적인 것 |
| 전제나 방향이 바뀐 기술 결정 | 리포지토리 문서가 이미 설명하는 구조 |

**원인이 같으면 한 항목이다.** 증상이 두 군데에서 보였다고 두 항목으로 쓰지 않는다. 쓰기 전에
기존 항목을 훑어 같은 뿌리가 있으면 그쪽을 고쳐 쓴다. 항목이 늘어나는 것보다 중복이 더 나쁘다.

**해결된 항목은 결과를 갱신한다.** `미해결` 로 적어둔 것이 끝나면 `R` 을 실제 결과로 고친다.
옛날에 미해결로 남은 줄이 쌓이면 노트를 믿을 수 없게 된다.

한 건이 끝나면 **먼저 기록하고 그 다음 보고한다.** 사용자가 따로 요청하지 않아도 기록한다.

---

## 계획 문서

구현 지시를 받으면 바로 코드를 고치지 않고 계획을 먼저 보여 주고 승인을 받는다.
여러 단계에 걸친 큰 작업은 계획을 `docs/exec-plans/active/YYYYMMDD-<주제>.md` 에 둔다. 끝나면 회고를 Obsidian 에 남기고 계획 문서를 정리한다.

---

## 알려진 환경 제약

- **이 PC 에서 testcontainers 통합 테스트가 안 돈다.** Docker Engine 29 와 맞지 않는다.
  스프링 컨텍스트 로딩 실패가 뜨면 **내 변경 탓으로 오진하지 말고** CI 에서 확인한다.
- 작업은 git worktree(`orca/workspaces/FunGame/<이름>`)에서 이뤄질 수 있다. 원본 체크아웃으로 `cd` 하지 않는다.
- git stash 스택은 모든 worktree 가 공유한다. 맨손 `git stash` / `git stash pop` 을 쓰지 않는다.
