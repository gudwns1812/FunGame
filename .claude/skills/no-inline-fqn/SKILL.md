---
name: no-inline-fqn
description: 정규화된 이름(com.fungame.songquiz.domain.room.GamePlayer 처럼 패키지를 통째로 적은 것)을 코드 안에 그대로 쓰지 않는다. import 로 바꾼다. 커밋 전에 자동으로 검사되며, 검사에 걸렸을 때나 자바 코드를 새로 쓸 때 쓴다. 테스트 코드도 예외가 아니다.
---

# 정규화된 이름을 코드에 박지 않는다

```java
// 이렇게 쓰지 않는다
private final com.fungame.songquiz.domain.member.MemberReader reader = mock(...);
org.mockito.BDDMockito.given(reader.findMember(1L)).willReturn(member);

// 이렇게 쓴다
import com.fungame.songquiz.domain.member.MemberReader;
import static org.mockito.BDDMockito.given;
```

읽는 사람은 한 줄에서 **무엇을 하는지**를 봐야 하는데, 패키지 경로가 그 자리를 다 차지한다.
`import` 는 파일 맨 위에서 한 번만 읽히고 그 뒤로는 눈에 띄지 않는다.

**테스트도 똑같다.** 급하게 쓰다 보면 import 를 관리하기 귀찮아 패키지를 통째로 적게 되는데,
그 줄을 나중에 읽는 사람에게는 운영 코드와 똑같은 비용이다.

## 검사

커밋할 때 `commit-gate.sh` 가 자동으로 돌린다. 직접 돌리려면

```bash
bash .claude/skills/no-inline-fqn/scripts/find-inline-fqn.sh
```

이미 커밋한 뒤에 보려면 범위를 준다 — `find-inline-fqn.sh HEAD~1`.

**이번 변경이 새로 추가한 줄**만 본다. 원래 있던 것은 건드리지 않는다. 아직 추적되지 않은
새 파일은 통째로 본다.

## 고치는 법

1. 패키지를 지우고 단순 이름만 남긴다
2. 파일 맨 위에 `import` 를 더한다. 메서드·상수면 `import static`
3. 컴파일한다. 같은 단순 이름이 이미 다른 패키지에서 import 돼 있으면 충돌한다

## 예외는 하나 — 이름이 충돌할 때

한 파일에서 같은 단순 이름을 가진 두 클래스를 함께 써야 하면 한쪽은 정규화된 이름으로
쓸 수밖에 없다. 이때는 **혼자 판단해서 넘기지 말고 사용자에게 말한 뒤** 커밋 앞에
`INLINE_FQN=skip` 을 붙인다.

대개는 충돌이 아니라 그냥 import 를 안 쓴 것이다. 먼저 충돌이 맞는지 확인한다.

## 검사기가 못 보는 것

- 문자열 안의 패키지 이름은 일부러 거른다. `getPackageName().startsWith("com.fungame...")`
  같은 코드는 진짜 데이터다
- 여러 줄에 걸쳐 끊어 쓴 이름은 못 잡는다
- `.java` 만 본다
