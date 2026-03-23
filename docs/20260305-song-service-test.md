# SongServiceTest 작성 계획

## 1. 개요
최근 리팩토링된 `SongService.existSongQuiz` 로직(`existsByTitleContaining`)의 정확성을 검증하기 위해 단위 테스트 코드를 작성합니다.

## 2. 세부 작업 내용
### 1) 테스트 대상
- `com.fungame.songquiz.domain.SongService.existSongQuiz(String title)`

### 2) 테스트 시나리오
- **케이스 1 (정확한 일치)**: "밤편지"가 저장소에 있을 때 "밤편지"로 검색 시 `true` 반환 확인.
- **케이스 2 (부분 일치)**: "밤편지"가 저장소에 있을 때 "밤"으로 검색 시 `true` 반환 확인 (Containing 동작 검증).
- **케이스 3 (불일치)**: 저장소에 없는 제목 검색 시 `false` 반환 확인.

### 3) 기술 스택 및 환경
- **JUnit 5**: 테스트 프레임워크.
- **Mockito**: `SongRepository` 및 `YoutubeScraper` 모킹.
- **AssertJ**: 가독성 높은 검증문 작성.

## 3. 작업 담당 에이전트
- **테스트 코드 작성**: `backend-tdd`
- **검토**: `backend-reviewer`

## 4. 기대 결과
- `SongServiceTest.java` 파일 생성 및 모든 테스트 케이스 통과.
