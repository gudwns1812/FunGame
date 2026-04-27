# 정답자 랭킹 Shimmer 효과 추가 계획 (v3)

정답을 맞춘 사용자에게 실시간 랭킹창에서 시각적인 강조 효과(thin horizontal shimmer sweep)를 부여하여 사용자 경험을 개선합니다.

## 목표 (Goal)
- 라운드 종료 후 정답이 공개될 때, 정답을 맞춘 사용자의 랭킹 항목에 가로로 지나가는 빛 효과(shimmer)를 추가합니다.
- TDD 원칙을 준수하여 안정적인 컴포넌트 분리 및 리팩토링을 수행합니다.

## 배경 및 문맥 (Context)
- `Game.tsx`에서 랭킹 로직을 분리하여 유지보수성을 확보합니다.
- 성능 최적화를 위해 GPU 가속(`transform`)을 활용하며, 시각적 피로도를 고려한 애니메이션을 설계합니다.

## 핵심 변경 파일 (Key Files)
- `frontend/src/index.css`: Shimmer 애니메이션 및 `@theme` 설정.
- `frontend/src/components/RankingList.tsx`: 랭킹 목록 관리 컴포넌트.
- `frontend/src/components/RankingItem.tsx`: 개별 플레이어 항목 컴포넌트.
- `frontend/src/components/RankingItem.test.tsx`: 신규 컴포넌트 단위 테스트.
- `frontend/src/components/RankingList.test.tsx`: 신규 컴포넌트 단위 테스트.
- `frontend/src/components/Game.tsx`: 컴포넌트 연동.

## 설계 및 아키텍처 결정 사항 (Design Decisions)
- **CSS Animation**:
    - `::after` 가상 요소와 `transform: translateX(-100%)` -> `translateX(100%)` 애니메이션 사용.
    - `RankingItem`에 `overflow: hidden` 적용하여 효과가 영역 밖으로 나가지 않도록 함.
    - 시각적 피로도를 줄이기 위해 애니메이션은 라운드 종료 시 **1회** 또는 **제한된 횟수**로 수행되도록 설정 가능하나, 요구사항에 맞춰 시각적으로 명확한 sweep 효과 구현.
- **Winner Detection**:
    - `stripTag`를 사용하여 정규화된 닉네임 비교.
    - `winner.split(',').map(s => s.trim())` 방식을 고려하여 다중 우승자 가능성에 대비.
    - `winner === '없음'`인 경우 명시적 무시.
- **TDD (Test-Driven Development)**:
    - 컴포넌트 구현 전 테스트 케이스(Red)를 먼저 작성하고 구현(Green) 진행.

## 테스트 전략 (Testing Strategy)
- **단위 테스트 (RankingItem.test.tsx)**:
    - `isWinner` prop에 따라 `animate-shimmer` 클래스 적용 여부 확인.
    - `@media (prefers-reduced-motion)` 대응 여부 (CSS 레벨).
- **단위 테스트 (RankingList.test.tsx)**:
    - 플레이어 점수순 정렬 확인.
    - `roundEndInfo` 기반의 승자 식별 로직(`useMemo`) 검증.
- **수동 검증**: 
    - 크롬 개발자 도구의 'Rendering' 탭을 통해 'Paint flashing'을 확인하여 리페인트 최소화 여부 검증.

## 구현 단계 (Implementation Steps)

### 1단계: CSS 애니메이션 정의
`frontend/src/index.css`에 shimmer 효과를 위한 키프레임과 유틸리티 클래스 추가.

### 2단계: RankingItem 개발 (TDD)
1. `RankingItem.test.tsx` 작성: 우승자 여부에 따른 스타일 적용 테스트.
2. `RankingItem.tsx` 구현: 가시적 피드백과 `overflow: hidden` 포함.

### 3단계: RankingList 개발 (TDD)
1. `RankingList.test.tsx` 작성: 정렬 및 승자 식별 로직 테스트.
2. `RankingList.tsx` 구현: `useMemo`를 통한 최적화된 로직 적용.

### 4단계: Game 컴포넌트 연동 및 리팩토링
`Game.tsx`의 인라인 랭킹 로직을 제거하고 `RankingList`로 교체.

### 5단계: 최종 검증
모든 테스트 통과 확인 및 브라우저 성능/시각적 효과 최종 확인.
