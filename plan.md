# 작업 계획: 신고 창구를 만들고 접수 즉시 디스코드로 알린다 (#62)

브랜치: `feat/report-with-discord-notify`

이슈의 작업 목록을 커밋 다섯 개로 쪼갠다. 커밋마다 테스트가 모두 통과하는 상태를 유지한다.

## 1단계 — 콘텐츠 식별자를 도메인까지 싣는다

신고가 어느 행을 가리키는지 정하는 것이 먼저다. 이 단계는 신고 기능과 무관하게 혼자 배포할 수 있다.

- `Song` 에 `id` 를 추가하고 팩토리를 나눈다. `Song.of(...)` 는 저장 전(id 없음), `Song.stored(id, ...)` 는
  DB 에서 읽어온 곡이다. `SongReader.toDomain()` 만 `stored` 를 쓴다.
  `SongWriter.upsertByVideoLink()` · `SongUpsertDao` · `existsSameSong()` 는 저장 전 곡을 다루므로 건드리지 않는다.
- `CsQuestion` 도 같은 방식으로 `of` / `stored` 를 나누고 `CsQuestionReader.toDomain()` 이 `stored` 를 쓴다.
- `HangmanWord` 는 record 이므로 `HangmanWord(Long id, String value, int difficulty)` 가 된다.
- `HangmanQuiz.create(String answer)` → `HangmanQuiz.create(HangmanWord word)`. 지금은 정답 문자열만 받아서
  단어 id 를 실을 자리가 없다. 리더가 이미 `HangmanWord` 를 돌려주므로 그대로 넘긴다.
- `Quiz.getCurrentContentId()` 를 인터페이스에 추가하고 세 구현체를 채운다. `AbstractQuiz` 에 기본 구현을 두지 않는다.
  `SongQuiz` · `CsQuiz` 는 라운드 시작 전(`currentIdx == -1`)에 `null` 을 돌려준다.
  `getStatus()` 는 손대지 않는다 — 라운드 시작 전 가드는 이미 호출부가 하는 방식이고
  (`QuizGameService:176` 의 `currentRound >= 1 ? ... : null`), 신고 조립도 같은 방식으로 `isRoundStarted()` 를 본다.

**테스트**: `SongReaderTest` · `CsQuestionReaderTest` · `HangmanWordReaderTest` 에 "읽어온 것은 id 를 갖는다",
세 Quiz 구현체에 "현재 문제의 id 를 돌려준다 / 라운드 시작 전에는 null 이다".

## 2단계 — 신고를 받아 저장한다 (디스코드 없이)

- `core-enum` 에 `ReportSource(IN_GAME, LOBBY)` · `ReportReason(CONTENT_NOT_SHOWN, CONTENT_WRONG, HINT_WRONG,
  ANSWER_WRONG, ETC)` · `ReportStatus(OPEN, RESOLVED)` 를 추가한다.
- `V18__report.sql` 로 `report` 테이블을 만든다. `content_id` 에는 FK 를 걸지 않는다(행이 지워져도 신고는 읽혀야 한다).
  `member_id` 만 FK 를 건다.
- `ReportEntity` + `ReportRepository` (`db-core`). `PromotionRequestEntity` 와 같은 모양
  (`@EntityListeners(AuditingEntityListener.class)` + 정적 팩토리 + `@NoArgsConstructor(PROTECTED)`).
- `domain/report/Report` — 신고자와 사유, 그리고 접수 시점의 게임 컨텍스트 **스냅샷**을 든다.
- `domain/report/ReportContext` — 게임 컨텍스트만 따로 든 값 객체. 세 가지로 조립된다.
  | 접수 시점 | 담기는 것 |
  |---|---|
  | 라운드 진행 중 | contentId · gameType · category · round · content · answer · hint · roomId |
  | 게임 중이지만 라운드 시작 전 | gameType · category · roomId |
  | 대기실 / 로비 | roomId 또는 없음 |
- `ReportWriter`(저장) · `ReportReader`(같은 신고 존재 여부, 분당 접수 수) — public 메서드에 `@Transactional`
  (`TransactionBoundaryTest` 규칙).
- `ReportService.receive(memberId, command)` — 방 소속 검증 → 컨텍스트 조립 → 저장. 트랜잭션을 열지 않는다
  (같은 규칙의 반대쪽: Service 는 `requestReset` · `resetPassword` · `approveRequest` 외에는 `@Transactional` 금지).
- `ReportController` `POST /api/reports`. `SecurityConfig` 는 `anyRequest().authenticated()` 라 손대지 않는다.
- `ErrorCode` 에 `R001` · `R002` · `R003`, `ErrorType` 에
  `REPORT_RATE_LIMIT_EXCEEDED(429)` · `REPORT_NOT_IN_ROOM(403)` · `REPORT_DETAIL_REQUIRED(400)` 를 추가한다.

**테스트**: `ReportServiceTest`(컨텍스트 조립 3경우 · 남의 방 거절 · 분당 제한 · ETC 인데 detail 이 비면 거절),
`ReportPersistenceTest`(엔티티 왕복), `ReportControllerDocsTest`(Rest Docs).

## 3단계 — 디스코드로 보낸다

- 새 모듈 `backend:clients:client-discord`. `client-mail` 과 같은 꼴이되 AWS SDK 대신 `spring-web` 의 `RestClient`.
  - `DiscordWebhookSender` `@Profile("prod")` — 웹훅 URL 은 `client.discord.report-webhook-url`,
    값은 `${DISCORD_REPORT_WEBHOOK_URL}` 로만 들어온다. 기본값을 두지 않는다.
  - `DiscordEmbed(String title, List<Field> fields)` — 클라이언트 모듈 소유 모델. 도메인 타입은 모른다.
  - 실패(429 포함)는 `log.error` 로 남기고 예외를 밖으로 던지지 않는다 (`SesMailSender` 와 같은 이유).
  - 실제 호출 테스트는 `@Tag("external")`.
- `client-discord.yml` 을 모듈 리소스에 두고 `application.yml` 의 `spring.config.import` 에 추가한다.
- `domain/report/ReportNotifier`(포트) / `DiscordReportNotifier`(`@Profile("prod")`) /
  `LoggingReportNotifier`(`@Profile("!prod")`). `Report` → `DiscordEmbed` 변환은 어댑터가 한다.
- 전송은 어댑터 메서드에 `@Async`. `ReportService` 는 저장 뒤 한 번 부른다.
- 같은 회원 · 같은 `content_id` · 같은 사유의 중복은 **저장은 하고 전송만 건너뛴다**
  (`ReportReader.existsSame(...)`). `content_id` 가 없는 신고(로비)는 중복 판정을 하지 않는다.

**테스트**: `LoggingReportNotifierTest`, `DiscordReportNotifierTest`(Embed 조립 — 사유가 제목, 컨텍스트가 필드),
`ReportServiceTest` 에 "중복 신고는 저장되지만 전송되지 않는다".

## 4단계 — 화면

- `src/hooks/useReport.ts` — `submitReport(payload)` 하나. axios 호출을 화면에서 분리한다.
- `src/components/ReportModal.tsx` — 게임 종류에 따라 사유 문구가 바뀐다. **현재 게임 정보는 표시하지 않는다.**
  `ETC` 를 고를 때만 입력창이 열리고, 비어 있으면 접수하지 않는다. 나머지 사유는 선택 즉시 전송된다.
  마크업은 대기실 내보내기 모달과 같은 패턴(`WaitingRoom.tsx:102-124`).
- `GamePage` · `HangmanPage` 의 `TopBar` 우측에 신고 버튼. `HangmanPage` 는 지금 `roomId` 를 받지 않으므로
  `App.tsx` 에서 함께 내려준다.
- `src/pages/ReportPage.tsx` + `/report` 라우트(로그인 필요) + `SiteFooter` 의 `FOOTER_LINKS` 에 `문의·신고`.

**테스트**: `ReportModal.test.tsx`(사유 문구가 게임 종류를 따른다 · ETC 는 내용이 없으면 전송하지 않는다 ·
나머지는 선택 즉시 전송된다), `ReportPage.test.tsx`, `GamePage.test.tsx` · `HangmanPage.test.tsx`(버튼 → 모달).

## 5단계 — 문서와 배포

- `api/report.md` 신규, `index.adoc` 에 신고 절 추가.
- `SERVER_REQUIREMENTS.md` 에 `DISCORD_REPORT_WEBHOOK_URL` 을 적는다. 값은 저장소에 남기지 않는다.

## 이슈에서 정하지 않아 내가 정한 것

1. **로비 신고의 게임 종류**를 어떻게 보내나. 이슈의 요청 바디에는 `gameType` 이 없는데 로비 화면의
   "콘텐츠 오류 제보" 는 게임 종류를 고르게 되어 있다. → 요청에 **선택 항목 `gameType` 을 두고,
   `source=IN_GAME` 이면 무시한다**(서버가 세션에서 채운 값이 이긴다). 로비 신고는 `reason=ETC` + `detail` 필수,
   "콘텐츠 오류 제보" 면 `gameType` 이 함께 온다.
2. **분당 제한을 DB 카운트로 센다.** `report` 에 `created_at` 이 있으므로 인메모리 카운터를 새로 두지 않는다.
   재기동에도 제한이 유지되고, 셀 수 있는 상태가 한 곳에만 남는다. 기준 시각은 이미 쓰고 있는 `Clock` 빈.
3. **`getStatus()` 는 고치지 않는다.** 이슈가 지적한 `currentIdx == -1` 문제는 새로 만드는
   `getCurrentContentId()` 에서만 막고, 기존 메서드는 호출부가 가드하는 지금 방식을 유지한다.
   신고 조립도 `isRoundStarted()` 를 먼저 본다.
4. **유저 신고(비매너)와 관리자 조회 화면은 범위 밖.** 이슈 그대로다.

## 검증

- 백엔드: `./gradlew :backend:core:core-api:test`
- 프론트엔드: `npx vitest run`, `npx tsc --noEmit`, `npx vite build`
- 로컬 통합: 게임 진행 중 신고 → 서버 로그(`LoggingReportNotifier`)에 컨텍스트가 채워져 나오는지,
  대기실·로비에서 신고할 때 채워지는 범위가 표의 세 경우와 맞는지 확인한다.
