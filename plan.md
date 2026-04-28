# 랭킹 화면 뱃지 이미지 추가 계획

## 1. 목표 (Goal)
게임 결과 랭킹 리스트에서 1, 2, 3위 플레이어에게 각각 금, 은, 동 뱃지 이미지를 표시하여 시각적 효과를 강화합니다.

## 2. 배경 및 문맥 (Context)
- 현재 랭킹 리스트(`RankingItem`)는 순위를 `#1`, `#2`와 같은 텍스트로만 표시하고 있습니다.
- `frontend/src/images/` 디렉토리에 `first.png`, `second.png`, `third.png` 이미지가 준비되어 있습니다.

## 3. 핵심 변경 파일 (Key Files)
- `frontend/src/components/RankingItem.tsx`: 순위에 따른 이미지 렌더링 로직 추가.
- `frontend/src/components/RankingItem.test.tsx`: 뱃지 이미지 렌더링 여부를 검증하는 테스트 코드 추가.

## 4. 설계 및 아키텍처 결정 사항 (Design Decisions)
- 순위(`rank` prop)가 1, 2, 3인 경우에만 해당 이미지를 렌더링합니다.
- 이미지는 순위 텍스트(#rank) 대신 또는 옆에 표시하며, 적절한 크기(예: 24x24 또는 32x32)로 조정합니다.
- `img` 태그의 `alt` 속성을 통해 접근성을 보장합니다.

## 5. 테스트 전략 (Testing Strategy)
- **TDD 실천**: 실제 구현 전, `RankingItem.test.tsx`에 1, 2, 3위일 때 이미지가 존재하는지 확인하는 테스트 케이스를 먼저 작성합니다.
- `vitest`와 `React Testing Library`를 사용하여 DOM에 `img` 태그가 올바른 `src`와 함께 존재하는지 검증합니다.

## 6. 구현 단계 (Implementation Steps)

### 1단계: 테스트 코드 작성 (Red)
1. `RankingItem.test.tsx`에 1위, 2위, 3위 플레이어 렌더링 시 이미지가 포함되는지 확인하는 테스트 추가.
2. 테스트 실행 및 실패 확인.

### 2단계: 기능 구현 (Green)
1. `RankingItem.tsx`에서 이미지 파일들을 import.
2. `rank` 값에 따라 이미지를 조건부 렌더링하는 로직 추가.
3. CSS를 통한 레이아웃 조정.

### 3단계: 검증 및 리팩토링 (Refactor)
1. 테스트 재실행 및 통과 확인.
2. 코드 가독성 및 스타일 최적화.
