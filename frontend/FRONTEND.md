# 프론트엔드 작업 지침 (FRONTEND.md)

## 기술 스택

- **프레임워크:** React 19 (Vite)
- **언어:** TypeScript
- **스타일링:** Tailwind CSS 4 + `src/index.css` 의 `px-*` 컴포넌트 클래스
- **통신:** axios (REST), `@stomp/stompjs` + SockJS (WebSocket)
- **테스트:** Vitest + Testing Library

## 작업 지침

1. **구조:** 화면은 `src/pages`, 재사용 UI 는 `src/components`, 타입은 `src/types` 에 둡니다.
2. **로직은 훅으로:** 게임·방 상태와 서버 이벤트 처리는 `src/hooks` 로 분리합니다. 컴포넌트는 그리는 일에 집중합니다.
3. **스타일:** 새 색이나 버튼을 만들기 전에 `index.css` 의 `px-*` 클래스와 색 토큰을 먼저 씁니다.
4. **테스트 먼저:** 훅은 `renderHook` 과 `src/test/stompTestUtils.tsx` 의 STOMP 스텁으로 이벤트를 흘려 검증합니다.
5. **API 명세:** 서버와 주고받는 형식은 루트 [api/](../api/) 가 기준입니다.

## 명령

```bash
npm run dev        # 공용 개발 서버를 바라보고 기동
npm run dev:local  # localhost:8080 백엔드를 바라보고 5199 포트로 기동
npm test           # vitest run
npm run lint
npm run build      # tsc -b && vite build
```
