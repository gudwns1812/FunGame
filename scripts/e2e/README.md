# E2E

실제 브라우저로 화면을 띄워 도는 테스트. 단위 테스트와 인수 테스트가 못 보는 구간
— 브라우저 두 개가 같은 방에서 주고받는 흐름 — 을 확인한다.

## 준비

이 저장소는 `local` 프로파일이 인메모리 H2 로 단독 기동하므로 DB 를 따로 띄울 필요가 없다.

```bash
# 터미널 1 — 백엔드 (localhost:8080)
./gradlew :backend:core:core-api:bootRun --args='--spring.profiles.active=local'

# 터미널 2 — 프론트엔드 (localhost:5199)
cd frontend && npm run dev:local

# 터미널 3 — 최초 1회
cd scripts/e2e && npm install && npx playwright install chromium
```

## 실행

```bash
cd scripts/e2e

npm test           # headless
npm run test:headed   # 브라우저를 띄워 눈으로 본다
npm run test:ui       # 단계별로 멈춰가며 디버깅
npm run report        # 마지막 실행 리포트
```

다른 주소를 보려면 `E2E_BASE_URL` 을 준다.

## 이 테스트가 지키는 것

닉네임을 바꿨을 때 **로그아웃 없이** 바로 반영되는가.

| 테스트 | 막는 회귀 |
|---|---|
| 소켓을 끊지 않고 바꿔도 상대 화면의 채팅과 참가자 목록이 새 닉네임이다 | 채팅 닉네임을 STOMP 프린시펄에서 읽는 것 |
| 방에 있는 동안 닉네임을 바꿔도 로비로 튕기지 않는다 | 로비 이동 조건이 닉네임 비교인 것 |
| 로비 접속자 목록에도 새 닉네임이 바로 보인다 | 접속자 목록이 옛 값을 들고 있는 것 |

## 테스트를 고칠 때 — 소켓을 끊지 말 것

첫 번째 테스트가 노리는 버그는 **WebSocket 이 살아 있는 동안에만** 드러난다.
STOMP 프린시펄은 핸드셰이크 때 한 번 박히므로, `page.goto` 로 전체 페이지를 다시 띄우면
소켓이 새로 붙으면서 새 닉네임이 들어가 버그가 가려진다.

실제로 처음 쓴 판이 `page.goto('/mypage')` 를 쓰는 바람에, 고치기 전 코드에서도 초록불이
떴다. 로그인한 뒤에는 **화면 안의 버튼만 눌러** 이동해야 한다.

테스트를 추가하거나 고쳤으면 **고치기 전 코드에서 실제로 빨간불이 뜨는지** 확인한다.
통과만 보고 믿지 않는다.
