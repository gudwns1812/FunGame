# 작업 계획: 문서 구조 정리 (docs 폴더 활용)

## 배경 및 목표
루트 및 각 모듈 디렉토리에 흩어져 있는 상세 문서들을 `docs/` 폴더로 이동하여 디렉토리 구조를 깔끔하게 유지하고 문서 관리의 효율성을 높입니다.

## 작업 상세

### 1. 디렉토리 생성 및 파일 이동
- **백엔드 (`backend/docs/`)**:
  - `backend/docs/` 디렉토리를 생성합니다.
  - 다음 파일들을 이동합니다:
    - `BACKEND.md`
    - `GUIDELINE.md`
    - `api_spec.md`
    - `guideline.md` -> `frontend_integration.md` (이름 변경)
- **프론트엔드 (`frontend/docs/`)**:
  - 기존 `frontend/docs/` 폴더를 활용하여 다음 파일을 이동합니다:
    - `FRONTEND.md`

### 2. 전역 지침 (`GEMINI.md`) 업데이트
- 문서 위치가 변경됨에 따라 `GEMINI.md`에서 각 모듈의 가이드를 참조하는 경로를 수정합니다.
  - `backend/BACKEND.md` -> `backend/docs/BACKEND.md`
  - `frontend/FRONTEND.md` -> `frontend/docs/FRONTEND.md`

### 3. Git 반영
- 변경된 구조를 커밋하고 원격 저장소에 푸시합니다.
- 커밋 메시지: `chore: [common] 문서 구조 정리 (docs 폴더로 이동)`

## 예상 결과
- 각 모듈의 루트 디렉토리가 소스 코드와 설정 파일 위주로 정리됨.
- 모든 문서가 `docs/` 폴더 내에 모여 있어 관리가 용이해짐.

---
위 계획에 대해 리뷰 부탁드립니다. 승인해 주시면 작업을 시작합니다.
