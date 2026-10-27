---
name: log-message
description: 로그 메시지는 한국어 '~했다' 체로 쓰고, 이메일 · 비밀번호 · 토큰 · 닉네임 · 로그인 id 같은 개인정보를 남기지 않는다. 커밋 전에 자동으로 검사되며, 검사에 걸렸을 때나 log.info · log.warn · log.error 를 새로 쓸 때 쓴다.
---

# 로그 메시지는 한국어로, 개인정보 없이

```java
// 이렇게 쓰지 않는다
log.info("Broadcasting round start in room {}", roomId);
log.error("메일 발송에 실패했습니다. 수신자: {}", email, e);

// 이렇게 쓴다
log.info("라운드를 시작했다: 방 {}", roomId);
log.error("메일 발송에 실패했다. 제목: {}", subject, e);
```

로그는 그라파나에서 검색어로 찾는다. 영어와 한국어가 섞이면 찾을 때마다 두 언어를 떠올려야 한다.
문장 안의 `room {}` · `member {}` 도 `방 {}` · `회원 {}` 으로 쓴다. 어미는 `~했다` 체다.

## 남기지 않는 것과 남기는 것

| 남기지 않는다 | 남긴다 |
| --- | --- |
| 이메일, 로그인 id, 닉네임, 비밀번호, 토큰 · 재설정 링크, 채팅 내용, 사용자가 입력한 값 그대로 | `memberId` · `roomId` 같은 내부 식별자 |

요청 · 세션 · 회원 · 방은 MDC 가 이미 로그 줄에 붙인다(`LogContext`). 메시지에 다시 적지 않아도 되지만,
문맥이 없는 스레드에서 찍히는 줄이면 인자로 적는다.

`e.getMessage()` 를 통째로 남기면 그 안의 사용자 입력이 따라 나온다. `BindException` 은 필드 이름과
실패 이유만 남긴다(`ApiControllerAdvice`).

## 검사

커밋할 때 `commit-gate.sh` 가 자동으로 돌린다. 직접 돌리려면

```bash
bash .claude/skills/log-message/scripts/find-bad-log-messages.sh
```

이미 커밋한 뒤에 보려면 범위를 준다 — `find-bad-log-messages.sh origin/main`.

**이번 변경이 새로 추가한** `log.trace · debug · info · warn · error(` 호출만 본다. 여러 줄에 걸친 호출은
`);` 까지 이어 읽는다. 아직 추적되지 않은 새 파일은 통째로 본다.

- 첫 문자열에 영문자가 있는데 한글이 없으면 걸린다. `"{} : {}"` 처럼 자리표시자와 기호만 있으면 통과다
- 문자열 밖의 인자에 `email` · `password` · `token` · `nickname` · `loginId` 가 들어 있으면 걸린다

## 예외

검사가 틀렸다고 판단되면 **혼자 넘기지 말고 사용자에게 이유를 말한 뒤** 커밋 앞에
`LOG_MESSAGE=skip` 을 붙인다. 예를 들어 `tokenCount` 처럼 이름만 걸리는 경우다. 먼저 이름을 바꿀 수 있는지 본다.

## 검사기가 못 보는 것

- 이름으로 거른다. `send(String to, ...)` 의 `to` 처럼 이름에 드러나지 않는 개인정보는 못 잡는다
- 상수로 뺀 메시지(`log.info(MESSAGE, ...)`)는 내용을 보지 못한다
- 예외 메시지 안에 섞여 나오는 값(`MailSendException` 에 든 SMTP 응답의 주소 등)은 못 잡는다
- `.java` 만 본다
