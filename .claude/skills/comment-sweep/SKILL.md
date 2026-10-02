---
name: comment-sweep
description: 커밋 직전 주석 청소. git commit 을 실행하기 전에 반드시 쓴다. 이번 변경이 새로 추가한 주석을 찾아 전부 지운 뒤 커밋한다. 작업 중에 주석으로 설명을 적는 것은 괜찮지만 커밋에는 남기지 않는다. Use before every git commit in this repository - detect comments added by the pending change, strip them, then commit.
---

# 커밋 전 주석 청소

작업하면서 주석으로 설명을 적는 것은 괜찮다. **커밋에는 남기지 않는다.**
설명이 필요하면 커밋 메시지에 적는다. 코드와 커밋 메시지 양쪽에 같은 말을 쓰지 않는다.

`git commit` 을 부르기 전에 항상 이 순서를 밟는다. 사용자가 따로 요청하지 않아도 한다.

## 1. 검사

```bash
bash .claude/skills/comment-sweep/scripts/find-added-comments.sh
```

기본값은 **작업 트리(스테이징 포함) vs HEAD** 다. 이미 커밋한 뒤에 돌릴 때는 범위를 준다.

```bash
bash .claude/skills/comment-sweep/scripts/find-added-comments.sh HEAD~1
```

출력은 `파일:줄: 내용` 이고, 후보가 하나라도 있으면 종료 코드가 1 이다.

스크립트는 **이번 변경이 새로 추가한 줄**만 본다. 원래 있던 주석은 건드리지 않는다.
아직 git 이 추적하지 않는 **새 파일은 통째로** 본다. 새로 만든 파일의 주석도 결국 이번
변경이 추가한 것이라 같은 기준으로 지운다.

**코드 파일만 본다** — `.java` `.js` `.jsx` `.ts` `.tsx` `.gradle` `.kt` `.kts`.
`yml`, `properties`, `sh`, `Dockerfile`, `html`, `sql`, `md` 같은 설정·문서 파일은 검사하지 않는다.
그쪽에서는 주석이 정상적인 설명 수단이라 지울 이유가 없다.

**테스트 코드는 예외다.** `src/test/`, `*Test.java`, `*.test.ts`, `*.spec.ts`, `__tests__/` 는
검사하지 않는다. `// given` `// when` `// then` 같은 표시가 이 리포지토리 테스트의 관례라
지우면 오히려 형식이 갈린다. 제외한 파일 수는 스크립트가 알려준다.

## 2. 제거

후보를 전부 지운다. 주석만 지우고 코드는 그대로 둔다. 주석이 빠져 빈 줄이 둘 이상
연달아 남으면 하나로 줄인다.

**예외는 하나다 — 설명이 아니라 동작하는 지시문.** 지우면 빌드나 린트가 달라지는 것들이다.

- `// @ts-ignore`, `/* eslint-disable */`, `// noinspection`, `// NOSONAR`
- `<!-- prettier-ignore -->`, `# noqa`, `# type: ignore`
- 라이선스 헤더

이 목록 밖인데 지우면 안 될 것 같은 주석이 있으면 **혼자 판단해서 남기지 말고 사용자에게 묻는다.**

## 3. 다시 검사

```bash
bash .claude/skills/comment-sweep/scripts/find-added-comments.sh
```

`새로 추가된 주석 없음.` 이 나와야 한다. 남은 게 있으면 2로 돌아간다.

## 4. 깨지지 않는지 확인

주석을 지우다 코드를 건드렸을 수 있다. 바뀐 모듈의 테스트를 돌린다.

```bash
./gradlew :backend:core:core-api:test --tests "*<바꾼 클래스>Test*"
```

## 5. 커밋

지운 주석에 담겨 있던 설명 중 남길 가치가 있는 것은 커밋 메시지 본문으로 옮긴다.
그 다음 커밋한다. 커밋 자체는 사용자의 허락을 받고 한다.

## 스크립트가 못 보는 것

다음은 직접 확인한다.

- 문자열 리터럴 안의 `//` 를 주석으로 잘못 집을 수 있다. 후보는 판단해서 거른다.
- 여러 줄 블록 주석은 각 줄이 따로 잡힌다. 블록 전체를 지운다.
- 스크립트가 아는 확장자 밖의 파일은 검사하지 않는다. 그런 파일을 건드렸으면 눈으로 본다.
