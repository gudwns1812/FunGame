# 프로젝트 작업 진행 현황 (tasks.md)

이 문서는 Spring Rest Docs 도입 및 API 명세 동기화 작업의 단계별 진행 상황을 관리합니다.

## Phase 1: Spring Rest Docs 인프라 설정
- [x] `backend/build.gradle`에 Spring Rest Docs 및 Asciidoctor 관련 플러그인 설정
- [x] Rest Docs 의존성(`spring-restdocs-mockmvc`) 추가
- [x] 빌드 태스크(`asciidoctor`, `test`) 간의 의존성 및 출력 경로 설정
- [x] 프로젝트 빌드 및 설정 정상 작동 여부 확인

## Phase 2: API 테스트 코드 작성 및 스니펫 생성
- [x] Rest Docs 공통 설정을 위한 베이스 테스트 클래스(`RestDocsSupport`) 작성
- [x] `GameController` 관련 API 테스트 작성 (MockMvc 활용)
- [x] 테스트 실행 및 `build/generated-snippets` 생성 확인

## Phase 3: 명세서 분석 및 AI 최적화 문서 업데이트
- [x] 생성된 AsciiDoc 스니펫 분석 (실제 요청/응답 필드 확인)
- [x] 루트 `api/` 디렉토리의 Markdown 문서 업데이트
- [x] AI 가독성을 높이기 위한 문서 포맷팅 최적화

## Phase 4: 최종 검증 및 배포 준비
- [ ] 전체 빌드(`gradlew :backend:build`) 수행 및 테스트 통과 확인
- [ ] `GEMINI.md` 및 `GUIDELINE.md` 내용과 일치하는지 최종 검토
- [ ] 사용자 최종 승인 요청
- [ ] (승인 후) Git 커밋 및 푸시 수행

---
*마지막 업데이트: 2026-03-09*
