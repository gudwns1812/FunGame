# 공통 API 응답 구조 (common.md)

이 문서는 시스템 전반에서 사용되는 공통 응답 구조인 `ApiResponse`를 정의합니다. 실제 백엔드 코드와 100% 동기화된 명세입니다.

## 공통 응답 포맷 (JSON)

모든 API 요청에 대한 응답은 아래 형식을 따릅니다.

```json
{
  "result": "SUCCESS" | "FAIL",
  "data": Object | null,
  "error": {
    "code": "ErrorCode",
    "message": "String"
  } | null
}
```

### 필드 상세 설명

| 필드 | 타입 | 설명 |
| :--- | :--- | :--- |
| **`result`** | `String` | 요청 처리 결과 (`SUCCESS` 또는 `FAIL`). `ResultType` enum을 사용합니다. |
| **`data`** | `Object` | 요청이 성공했을 때 반환되는 비즈니스 데이터. 실패 시 `null`. |
| **`error`** | `Object` | 요청이 실패했을 때 반환되는 `ErrorMessage` 객체. 성공 시 `null`. |
| **`error.code`** | `String` | 비즈니스 에러 코드 (`ErrorCode` enum 명칭). 예: `G001`. |
| **`error.message`** | `String` | 사용자에게 보여줄 수 있는 상세 에러 메시지. |

---
- **성공 시**: `result`는 `"SUCCESS"`, `data`에 결과값이 담기며, `error`는 `null`입니다.
- **실패 시**: `result`는 `"FAIL"`, `data`는 `null`이며, `error` 객체에 상세 에러 정보가 포함됩니다.
