# README.md 작성 계획

## 1. 개요
현재 프로젝트(`FunGame`)의 전반적인 구조, 기술 스택, 주요 기능 및 실행 방법을 정리한 `README.md` 파일을 작성합니다.

## 2. 상세 작업 내용
`README.md`에 포함될 주요 섹션은 다음과 같습니다:

1.  **프로젝트 제목 및 소개**
    *   실시간 멀티플레이어 게임 플랫폼 `FunGame` 소개.
2.  **주요 기능**
    *   실시간 방 생성 및 입장 시스템.
    *   플레이어 준비(Ready) 상태 관리 및 게임 시작 로직.
    *   WebSocket 기반의 실시간 데이터 동기화.
    *   네트워크 불안정 시를 위한 재접속(Re-entry) 및 세션 유지 기능.
    *   게임 종료 후 결과 화면 및 대기실 복귀 시스템.
3.  **기술 스택**
    *   **Backend:** Java 17+, Spring Boot, Gradle, Spring Rest Docs.
    *   **Frontend:** React, TypeScript, Vite, Vanilla CSS.
    *   **Communication:** WebSocket (STOMP).
4.  **프로젝트 구조**
    *   모노레포 구조 (`backend/`, `frontend/`, `api/`, `docs/`) 설명.
5.  **실행 방법**
    *   백엔드 및 프론트엔드 실행을 위한 기본 명령어 가이드.
6.  **개발 가이드라인**
    *   `GEMINI.md`, `BACKEND.md`, `FRONTEND.md` 참조 안내.

## 3. 일정 및 절차
1.  (현재) `plan.md` 작성 및 승인 요청.
2.  `README.md` 초안 작성.
3.  프로젝트 루트에 `README.md` 파일 생성.

---
위 계획에 대해 승인해 주시면 작업을 시작하겠습니다.
