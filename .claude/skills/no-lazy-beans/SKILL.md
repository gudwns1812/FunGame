---
name: no-lazy-beans
description: 스프링 빈을 만들거나 프레임워크 빈에 의존할 때 쓴다. @Lazy 를 쓰지 않고, 남의 @Lazy 빈에 기대지 않는다. 지표 등록·워밍업·헬스체크처럼 기동 시점에 있어야 하는 것이 조용히 비는 사고를 막는다. Use when wiring Spring beans or depending on framework-provided beans - forbid @Lazy and never rely on a lazily created bean for anything that must exist at startup.
---

# 지연 생성 금지

**기동 시점에 없는 빈은 없는 빈이다.**

기동 때 한 번 훑고 지나가는 것들이 있다. 메트릭 바인딩, 워밍업, 헬스 인디케이터 등록,
이벤트 리스너 수집. 그 시점에 빈이 만들어져 있지 않으면 **에러 없이 그냥 건너뛴다.**
나중에 빈이 생겨도 이미 지나간 뒤라 끝까지 붙지 않는다.

## 검사

```bash
bash .claude/skills/no-lazy-beans/scripts/find-lazy.sh
```

이번 변경이 새로 추가한 `@Lazy` 와 `spring.main.lazy-initialization` 을 찾는다.
후보가 있으면 종료 코드가 1 이다.

## 남의 @Lazy 빈에 기대지 않는다

스크립트는 우리 코드만 본다. **의존하는 프레임워크 빈이 `@Lazy` 인지는 직접 확인한다.**
자동 구성 소스를 열어 보는 게 가장 빠르다.

```bash
SRC=$(find ~/.gradle/caches -name "spring-boot-autoconfigure-*-sources.jar" | head -1)
unzip -p "$SRC" "org/springframework/boot/autoconfigure/<경로>.java" | grep -nB3 "<빈 이름>"
```

`@Lazy` 였다면 **그 빈을 우리가 갖는다.** 주입해서 강제로 깨우는 우회를 쌓지 않는다.
우회는 원인을 남겨둔 채 코드만 늘린다.

가져올 때는 **프레임워크가 달던 이름과 별칭을 그대로 붙인다.** 이름이 바뀌면 그걸
이름으로 찾던 쪽이 조용히 다른 것으로 폴백한다.

```java
@Bean(name = {
        TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
        AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME})
public ThreadPoolTaskExecutor applicationTaskExecutor(...) { ... }
```

우리 빈이 생기면 대부분의 자동 구성은 `@ConditionalOnMissingBean` 으로 물러난다.
물러나면서 **같이 사라지는 것**(별칭, 프로퍼티 바인딩, 메트릭 등록)이 무엇인지 확인한다.

## 검증

기동과 함께 만들어졌는지를 테스트로 못 박는다. `@Lazy` 로 돌아가면 깨진다.

```java
contextRunner.run(context ->
        assertThat(context.getBeanFactory().containsSingleton("<빈 이름>")).isTrue());
```

**격리된 `ApplicationContextRunner` 의 결론을 전체 앱에 그대로 옮기지 않는다.**
러너에 올린 자동 구성만 보이므로 같은 타입 빈이 실제로 몇 개인지, 어느 것이 뽑히는지가
다르다. 마지막은 실제로 띄워서 확인한다.

```bash
./gradlew :backend:core:core-api:bootRun --args='--spring.profiles.active=local --server.port=18080 --management.server.port=18081'
curl -s http://localhost:18081/actuator/prometheus | grep -E '^executor_|^fungame_'
```

로컬 통합 테스트가 이 PC 에서 돌지 않으므로(Docker) **이 부팅이 유일한 그물이다.**
확인 뒤에는 포트를 쥔 프로세스를 반드시 정리한다. `pkill` 은 Windows 자바 프로세스를
죽이지 못한다.

```bash
netstat -ano | grep ":18081" | grep LISTENING | awk '{print $5}' | xargs -r -I{} taskkill //PID {} //F
```
