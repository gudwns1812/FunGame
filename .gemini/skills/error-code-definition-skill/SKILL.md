---
name: error-code-definition-skill
description: 비즈니스 로직 예외 발생 시 CoreException과 ErrorType, ErrorCode를 활용하여 표준화된 에러를 처리하는 지침
---

# Error Code Definition Skill

이 스킬은 백엔드 로직에서 예외 상황(예: 데이터 없음, 유효성 검증 실패, 권한 부족 등)을 처리할 때, 하드코딩된 예외 대신 프로젝트의 표준 예외 아키텍처를 준수하기 위한 워크플로우입니다.

## Workflow

### 1. 예외 발생 시 표준 Exception 사용 (Hardcoding 금지)

- 비즈니스 로직 처리 중 예외를 발생시켜야 할 때 반드시 프로젝트의 최상위 커스텀 예외인 `CoreException`을 사용해야 합니다.

### 2. ErrorType 및 ErrorCode 정의

- 모든 에러는 `ErrorType` 열거형에 정의된 상수를 기반으로 합니다.
- `ErrorType`은 다음의 정보를 포함합니다:
    - `HttpStatus`: 응답 코드
    - `LogLevel`: 로깅 레벨 (DEBUG, WARN, ERROR)
    - `Message`: 사용자에게 노출될 메시지
- 새로운 종류의 에러가 필요하다면 기존 도메인에 맞는 `ErrorType`을 추가하거나 확장하십시오.

### 3. 예외 던지기 (Throwing Exception)

- `CoreException` 발생 시 `ErrorType`의 상수를 인자로 넘깁니다.

```java
// 올바른 예 (스킬 적용)
if (status == GameRoomStatus.PLAYING) {
    throw new CoreException(ErrorType.GAME_ALREADY_PLAYING);
}
```
