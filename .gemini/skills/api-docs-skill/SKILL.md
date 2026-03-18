---
name: api-docs-skill
description: Spring REST Docs로 API 문서를 생성하고 관리한다. 새 API 엔드포인트 개발, 기존 API 명세 수정, 테스트 기반 문서 스니펫 작성, 문서 빌드 및 배포 설정이 필요할 때 사용한다. @WebMvcTest + @AutoConfigureRestDocs 조합을 기본으로 하며, 성공/실패 케이스 스니펫 작성과 .md 산출물 배포까지 전 과정을 담당한다.
---

# Spring REST Docs 문서화 가이드

## 핵심 원칙

- **테스트가 문서다**: 테스트 코드 없이 문서를 손으로 작성하지 않는다.
- **컨테이너 최소화**: `@SpringBootTest`보다 `@WebMvcTest`를 기본으로 선택한다.
- **성공/실패 모두 문서화**: 200 OK만 있는 문서는 불완전한 문서다.

---

## 1. 테스트 클래스 기본 구조

```java
@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerDocsTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void createUser_success() throws Exception {
        given(userService.create(any())).willReturn(userFixture());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "alice@example.com", "name": "Alice" }
                """))
            .andExpect(status().isCreated())
            .andDo(document("users/create",
                requestFields(
                    fieldWithPath("email").description("이메일 주소"),
                    fieldWithPath("name").description("사용자 이름")
                ),
                responseFields(
                    fieldWithPath("data.id").description("생성된 사용자 ID"),
                    fieldWithPath("data.email").description("이메일 주소"),
                    fieldWithPath("data.createdAt").description("생성 시각 (ISO 8601)")
                )
            ));
    }

    @Test
    void createUser_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "invalid", "name": "" }"""))
            .andExpect(status().isUnprocessableEntity())
            .andDo(document("users/create-error",
                responseFields(
                    fieldWithPath("error.code").description("에러 코드"),
                    fieldWithPath("error.message").description("에러 메시지"),
                    fieldWithPath("error.details[].field").description("유효성 실패 필드"),
                    fieldWithPath("error.details[].message").description("실패 이유")
                )
            ));
    }
}
```

**`@SpringBootTest` 허용 예외 조건**: Security 필터 체인 전체 테스트, 외부 라이브러리 통합(e.g. QueryDSL 동적 쿼리)처럼 스텁 설정이 테스트 코드보다 복잡해지는 경우에만 허용한다.

---

## 2. 스니펫 작성 기준

### 필수 스니펫 (모든 엔드포인트)

| 스니펫             | 메서드           | 설명                     |
| ------------------ | ---------------- | ------------------------ |
| `request-fields`   | POST, PUT, PATCH | 요청 바디 필드           |
| `response-fields`  | 전체             | 응답 바디 필드           |
| `path-parameters`  | 있을 때          | `/users/{id}` 형태       |
| `query-parameters` | 있을 때          | 필터, 정렬, 페이지네이션 |

### 에러 케이스 필수 문서화

```java
// 반드시 포함해야 할 에러 케이스
// - 400 / 422: 입력값 유효성 실패
// - 401: 인증 누락
// - 403: 권한 없음
// - 404: 리소스 없음
// - 409: 중복/충돌 (해당하는 경우)
```

---

## 3. 응답 포맷 표준

스니펫 필드 경로는 아래 포맷을 기준으로 작성한다.

```json
// 단건 성공
{ "data": { "id": "abc-123", "email": "..." } }

// 목록 성공
{
  "data": [...],
  "meta": { "total": 142, "page": 1, "perPage": 20, "totalPages": 8 },
  "links": { "self": "...", "next": "...", "last": "..." }
}

// 에러
{
  "error": {
    "code": "validation_error",
    "message": "요청 유효성 검사 실패",
    "details": [
      { "field": "email", "message": "올바른 이메일 형식이 아닙니다", "code": "invalid_format" }
    ]
  }
}
```

---

## 4. 문서 산출물 배포

### 경로 규칙

```
빌드 결과:  build/docs/asciidoc/{api-name}.html
배포 경로:  {project-root}/api/{api-name}.md
예시:       로그인 API → root/api/user.md
```

### build.gradle 설정 확인

아래 태스크가 없으면 추가를 제안한다.

```groovy
tasks.register('copyDocument', Copy) {
    dependsOn asciidoctor
    from "${asciidoctor.outputDir}"
    into "${rootDir}/api"
    include '*.html'
    rename { it.replace('.html', '.md') }
}

build.finalizedBy copyDocument
```

---

## 5. 체크리스트

새 엔드포인트 문서화 완료 전 확인:

- [ ] `@WebMvcTest` + `@AutoConfigureRestDocs` 구성
- [ ] 성공 케이스 스니펫 작성
- [ ] 주요 에러 케이스(400/401/403/404) 스니펫 작성
- [ ] 응답 포맷이 표준 구조(`data` / `error` 래퍼) 준수
- [ ] `copyDocument` 태스크로 `api/` 경로에 배포 확인
