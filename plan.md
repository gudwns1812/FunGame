# 작업 계획: 마이페이지에 목차를 두고 신고 처리 현황을 보여준다

브랜치: `feat/report-with-discord-notify` (#62 위에 이어서)

지금은 신고를 접수하면 디스코드로만 흘러가고, 신고한 사람은 그게 어떻게 됐는지 알 방법이 없다.
접수한 사람은 처리 현황을, 관리자는 답변할 자리를 갖게 한다.

## 가장 먼저 정한 것 — 신고자에게 정답과 힌트를 보여주지 않는다

`report` 행에는 접수 시점의 **정답 · 힌트 · 유튜브 링크 · 문제 식별자**가 스냅샷으로 들어 있다.
이걸 "내 문의" 화면에 그대로 내려주면 **라운드 진행 중에 신고 버튼을 눌러 정답을 읽는 치트 통로**가 된다.

그래서 응답 모델을 둘로 나눈다.

| | 신고자 (`GET /api/reports/mine`) | 관리자 (`GET /api/admin/reports`) |
|---|---|---|
| 사유 · 접수 시각 · 상태 | O | O |
| 본인이 직접 쓴 내용 | O | O |
| 게임 종류 | O | O |
| 관리자 답변 | O | O |
| 신고자 닉네임 | — (자기 것만 보므로) | O |
| 방 · 라운드 · 문제 · **정답** · **힌트** · 문제 식별자 | **X** | O |

관리자 답변은 신고자에게 그대로 보이므로, 답변 입력창 옆에 "이 답변은 신고자에게 보입니다" 를 적어
사람이 정답을 옮겨 적지 않게 한다. 기계로 막을 수 있는 건 위 표까지다.

## 1단계 — 처리 상태와 답변을 받을 자리를 만든다 (백엔드)

**상태**

- `ReportStatus` 는 지금의 `OPEN`(접수) · `RESOLVED`(처리 완료) 두 개를 그대로 쓴다.
  관리자는 둘 사이를 왕복할 수 있다. "왜 그렇게 처리했는지" 는 상태가 아니라 답변 문장이 말한다.

**답변**

- 답변은 관리자만 남긴다. 신고자는 읽기만 한다. 더 할 말이 있으면 새로 접수한다.
  덕분에 `POST .../comments` 는 `/api/admin/**` 아래에만 두면 되고 권한 가드가 하나로 끝난다.

- `V19__report_comment.sql` — `report_comment(id, report_id, member_id, content, created_at)`.
  `report_id` · `member_id` 는 FK 를 건다. `report` 를 지우면 답변도 함께 지워진다.
- `ReportCommentEntity` + `ReportCommentRepository` (`db-core`).
  `ReportEntity` 에 `@OneToMany(mappedBy = "report")` 를 두고 조회는 `join fetch` 로 한 번에 긁는다.

**도메인**

- `Report` 에 `id` · `createdAt` · `reporterNickname` · `comments` 를 싣고 `restore(...)` 를 더한다.
  지금 `Report` 는 접수 전용이라 `open(...)` 만 있다.
- `ReportComment` — `write(reportId, authorId, content)` 가 빈 내용을 거절한다(`R003` 재사용).
- `ReportWriter.append` 가 저장한 신고 번호를 돌려주고, 서비스가 그 번호를 실어 알린다.
  지금은 디스코드 메시지에 신고 번호가 없어 관리자가 어느 행을 열어야 할지 알 수 없다.
- `ReportWriter.appendComment` · `changeStatus`, `ReportReader.findMine` · `findAll` · `findById`.

**API**

| Method | Path | 권한 | 하는 일 |
|---|---|---|---|
| `GET` | `/api/reports/mine` | 로그인 | 내가 접수한 신고와 상태 · 답변 |
| `GET` | `/api/admin/reports` | ADMIN 이상 | 전체 신고. `?status=OPEN` 으로 걸러본다 |
| `POST` | `/api/admin/reports/{id}/comments` | ADMIN 이상 | 답변 남기기 |
| `PATCH` | `/api/admin/reports/{id}/status` | ADMIN 이상 | 상태 바꾸기 |

`/api/admin/**` 은 `SecurityConfig` 가 이미 `hasAnyRole("ADMIN", "MASTER")` 로 막고 있어 설정을 건드리지 않는다.

**테스트**

- `ReportServiceTest` — 내 신고만 돌려준다 / 답변이 빈 내용이면 거절한다 / 상태가 바뀐다 /
  알림에 신고 번호가 실린다
- `ReportPersistenceTest` — 답변이 행으로 남고 신고와 함께 읽힌다, 신고를 지우면 답변도 지워진다
- `ReportControllerDocsTest` · `AdminReportControllerDocsTest` — Rest Docs, 그리고
  **신고자 응답에 정답 · 힌트가 없다**는 것을 테스트로 고정한다

## 2단계 — 마이페이지에 목차를 두고 내 문의를 붙인다 (프론트)

지금 `MyPage.tsx` 는 프로필 · 닉네임 변경 · 승급 신청이 한 파일에 쌓여 있다. 목차가 생기면
화면이 셋으로 늘어나므로 먼저 쪼갠다.

- `src/pages/MyPage.tsx` — 좌측 목차 + 우측 본문의 껍데기. 목차 항목은 URL 로 주소를 갖는다.
  `/mypage` (내 정보) · `/mypage/reports` (내 문의) · `/mypage/inquiries` (문의 관리).
  관리자가 문의 관리 화면을 북마크할 수 있어야 하므로 화면 상태가 아니라 경로로 나눈다.
  `App.tsx` 의 `/mypage` 라우트를 `/mypage/*` 로 바꾸고 안에서 다시 나눈다.
- `src/components/mypage/MyPageNav.tsx` — 목차. `ADMIN` 이상일 때만 문의 관리를 넣는다.
  좁은 화면에서는 좌측 세로 목차가 위쪽 가로 탭으로 바뀐다.
- `src/components/mypage/ProfileSection.tsx` — 지금 `MyPage` 본문을 그대로 옮긴다. 동작은 안 바꾼다.
- `src/components/mypage/MyReportsSection.tsx` — 접수 목록. 사유 · 게임 종류 · 접수 시각 · 상태 칩 ·
  내가 쓴 내용 · 관리자 답변. 답변이 없으면 "아직 확인 중입니다".
- `src/hooks/useMyReports.ts` — `GET /api/reports/mine`.
- `src/types/report.ts` 에 응답 타입과 상태 라벨을 더한다.

**테스트**

- `MyPageNav.test.tsx` — USER 에게는 문의 관리가 보이지 않고 ADMIN 에게는 보인다
- `MyReportsSection.test.tsx` — 상태를 한글로 보여준다 / 답변이 없으면 확인 중이라고 알린다 /
  접수한 게 없으면 빈 화면을 보여준다
- `MyPage.test.tsx` — 경로에 따라 해당 절이 열린다, USER 가 `/mypage/inquiries` 로 들어오면 되돌린다

## 3단계 — 문의 관리 화면 (프론트, ADMIN 이상)

- `src/components/mypage/ReportAdminSection.tsx` — 상태 필터, 신고 카드에 컨텍스트 전부(정답 · 힌트 포함),
  답변 입력창, 상태 변경 버튼. 답변 입력창 옆에 신고자에게 보인다는 것을 적는다.
- `src/hooks/useReportAdmin.ts` — 목록 조회 · 답변 · 상태 변경.

**테스트**

- `ReportAdminSection.test.tsx` — 상태 필터가 조회 조건으로 넘어간다 / 답변을 남기면 목록을 다시 읽는다 /
  빈 답변은 보내지 않는다 / 상태를 바꾸면 목록을 다시 읽는다

## 4단계 — 문서

- `api/report.md` — 세 API 와 두 응답 모델의 차이, 정답 · 힌트를 신고자에게 내려주지 않는 이유
- `index.adoc` — 신고 절에 조회 · 답변 · 상태 변경 추가
- `backend/ARCHITECTURE.md` — `report` aggregate 설명에 답변과 처리 상태를 더한다

## 검증

- 백엔드: `./gradlew clean build`
- 프론트: `npx vitest run`, `npx tsc --noEmit`, `npx eslint <새 파일>`, `npx vite build`
