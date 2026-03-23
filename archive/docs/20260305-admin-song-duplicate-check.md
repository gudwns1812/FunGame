# AdminSongPage 노래 제목 중복 체크 기능 추가 계획 (완료)

## 1. 개요
관리자가 노래를 등록하기 전에 이미 등록된 노래인지 확인할 수 있는 중복 체크 기능을 `AdminSongPage.tsx`에 추가합니다.

## 2. 상세 작업 내용

### 1) UI 수정 (Tailwind CSS 활용)
- 노래 제목 입력 필드와 '중복 확인' 버튼을 수평 배치 (`flex gap-2`).
- '중복 확인' 버튼 디자인: 기존 '추가' 버튼과 유사한 스타일 (`bg-slate-800`, `hover:bg-slate-700`, `rounded-xl`).
- 입력 필드 하단에 상태 메시지 표시 영역 추가.
  - 중복 시: `text-red-400` ("이미 등록된 노래입니다.")
  - 사용 가능 시: `text-green-400` ("등록 가능한 노래입니다.")

### 2) 상태 관리 (React useState)
- `isCheckingDuplicate`: API 호출 중 로딩 상태 제어.
- `duplicateStatus`: 중복 여부 상태 (`'idle' | 'checking' | 'available' | 'duplicate'`).
- `duplicateMessage`: 화면에 표시할 메시지.

### 3) API 연동 (Axios)
- 호출 주소: `GET /api/admin/songs?title={title}`
- 로직:
  - 제목이 비어있는 경우 버튼 비활성화.
  - API 호출 결과(`boolean`)가 `true`이면 '중복', `false`이면 '사용 가능'.
  - 제목 입력 값이 변경되면 기존 중복 체크 상태 초기화.

## 3. 작업 담당 에이전트
- 구현: `frontend-builder` (수행 완료)
- 검증: `frontend-tester` (수행 완료)

## 4. 실행 결과
- `AdminSongPage.tsx` 수정 완료.
- 중복 체크 기능 정상 구현 및 스타일 적용 완료.
- 제목 변경 시 상태 초기화 로직 포함.
