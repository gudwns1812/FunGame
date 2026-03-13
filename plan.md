# 음악 퀴즈 소리 재생 문제 해결 계획 (Throttling 대응)

## 문제 분석
- 현상: `react-player` 교체 후 소리가 나지 않음 (콘솔 에러 없음).
- 원인: 
    - `top: -9999px` 설정으로 인해 브라우저가 플레이어를 '비가시적'인 것으로 판단, 리소스 최적화를 위해 재생을 중단함.
    - 자동 재생(Autoplay) 정책보다는 브라우저의 미디어 최적화(Media Throttling) 문제로 보임.

## 해결 전략
1. **가시성 확보 (Visibility):** 플레이어를 화면 범위 내(`top: 0`)에 위치시키되, `opacity: 0`, `pointer-events: none`을 통해 사용자에겐 숨김.
2. **명시적 설정:** `volume`, `playsinline`, `muted` 속성을 명확히 정의.
3. **상태 추적:** 플레이어의 `onReady`, `onStart`, `onError` 이벤트를 콘솔에 로깅하여 실제 동작 여부 확인.

## 작업 단계
1. `frontend/src/components/Game.tsx`의 `ReactPlayer` 렌더링 부분 스타일 및 속성 수정.
2. `useGameLogic.ts`에서 상태 전달이 제대로 이루어지는지 재확인 (이미 확인됨).

## 검증 계획
- `ROUND_START` 시 콘솔에 `[Player] Ready` 및 `[Player] Started` 로그가 찍히는지 확인.
- 실제 소리 출력 확인.
