# 작업 계획: Spring Rest Docs를 통한 API 명세 동기화

## 배경 및 목표
현재 API 명세와 실제 코드가 불일치하는 문제를 해결하기 위해, Spring Rest Docs를 도입하여 코드를 기반으로 한 정확한 API 문서를 생성하고, 이를 AI가 읽기 쉬운 Markdown 형식으로 `api/` 폴더에 재구성합니다.

## 작업 상세

### 1. Spring Rest Docs 설정 (`backend/build.gradle`)
- **의존성 추가**: `spring-restdocs-mockmvc`, `asciidoctor-gradle-plugin` 등을 추가합니다.
- **빌드 태스크 설정**: 테스트 실행 시 스니펫이 생성되도록 `test` 태스크를 설정하고, `asciidoctor` 태스크를 구성합니다.

### 2. API 문서화를 위한 테스트 코드 작성
- **`GameControllerTest`**: 방 생성, 목록 조회, 게임 시작 등의 API에 대해 `MockMvc`와 Rest Docs를 사용한 테스트를 작성합니다.
- **필드 및 헤더 명세**: 실제 코드의 DTO 및 헤더 정보를 바탕으로 요청/응답 필드에 대한 명세를 상세히 작성합니다.

### 3. 문서 생성 및 스니펫 분석
- `./gradlew :backend:test` 실행을 통해 `build/generated-snippets` 경로에 스니펫을 생성합니다.
- 생성된 스니펫(HTTP request/response, fields 등)을 분석하여 최신 API 정보를 확보합니다.

### 4. `api/` 폴더의 Markdown 문서 업데이트
- **`api/room.md`, `api/game.md`, `api/common.md`** 등 기존 문서들을 생성된 스니펫을 바탕으로 수정합니다.
- AI가 읽기 쉽도록 명확한 필드 설명, 제약 조건, 예시 데이터를 포함합니다.

### 5. Git 반영 (사용자 승인 후)
- 모든 변경 사항을 커밋하고 푸시합니다.

## 예상 결과
- 실제 동작하는 코드와 문서 간의 100% 일치 보장.
- AI가 API를 활용할 때 정확한 명세를 기반으로 제안 및 구현 가능.

---
위 계획에 대해 리뷰 부탁드립니다. 승인해 주시면 Spring Rest Docs 설정을 시작하겠습니다.
