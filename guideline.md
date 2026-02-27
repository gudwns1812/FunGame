# Song Quiz Game Frontend Implementation Guideline (React + Tailwind CSS)

본 문서는 `FunGame` 서버와 연동하여 동작하는 웹 프론트엔드 개발을 위한 가이드라인입니다. 스타크래프트 유즈맵(UMS) 감성을 살린 디자인과 실시간 통신을 중심으로 설계되었습니다.

---

### 1. 기술 스택 (Tech Stack)

- **Framework**: React (Vite 기반 권장)
- **Styling**: Tailwind CSS
- **Icon**: Lucide React (또는 전용 스타크래프트 스타일 아이콘)
- **State Management**: Zustand (또는 Context API) - 가벼운 전역 상태 관리
- **Real-time Communication**: `@stomp/stompjs` & `sockjs-client`
- **HTTP Client**: `axios`

---

### 2. UI/UX 디자인 컨셉: 스타크래프트 유즈맵 (StarCraft UMS)

스타크래프트의 브리핑 화면, 대기방, 인게임 UI를 재해석하여 구현합니다.

#### 주요 시각적 요소
- **Color Palette**: 
  - `Dark Blue / Slate-900`: 기본 배경색
  - `Cyan / Blue-400`: 텍스트 및 하이라이트 (테란/프로토스 느낌)
  - `Green / Lime-400`: 플레이어 목록 및 성공 메시지
  - `Yellow / Amber-400`: 시스템 메시지 및 경고
- **Typography**: 고정폭(Monospace) 폰트 또는 'Neo-Classic' 폰트 사용.
- **Components**:
  - `Border`: 이중 테두리와 네 모서리에 꺾쇠(`┌ ┐`) 디자인 적용.
  - `Transparency`: 반투명한 검은색 배경 패널.
  - `Button`: 클릭 시 짙은 청색으로 변하는 금속 질감의 사각형 버튼.

---

### 3. 주요 화면 구성 (Key Screens)

#### A. 로비 (Lobby / Briefing Room)
- **기능**: 방 목록 조회(현재 API 미구현 시 직접 입장), 방 생성.
- **UI**: 스타크래프트 미션 브리핑 화면 컨셉.
- **API**: `POST /game/room` (방 생성)

#### B. 게임 대기실 (Game Room - Waiting)
- **기능**: 플레이어 리스트, 방장 표시, 게임 시작 버튼(방장만).
- **UI**: 8개 슬롯이 있는 대기방 화면.
- **WebSocket 구독**: `/subscribe/room/{roomId}`
- **메시지 타입 처리**:
  - `PLAYER_JOIN`: 새 플레이어 슬롯 추가.
  - `PLAYER_LEAVE`: 플레이어 슬롯 제거.
  - `HOST_CHANGE`: 왕관(방장) 아이콘 이동.
  - `GAME_START`: 인게임 화면으로 전환.

#### C. 인게임 (In-Game - Playing)
- **기능**: 유튜브 영상 재생(소리 위주), 채팅창(정답 입력), 타이머 표시, 실시간 랭킹.
- **UI**:
  - **상단**: `TIMER_TICK` 정보를 기반으로 한 게이지 바(또는 숫자).
  - **중앙**: 유튜브 임베드 (영상은 가리거나 최소화, 소리만 들리게 설정 가능).
  - **우측**: 실시간 점수판 (ZSet 랭킹 기반).
  - **하단**: 채팅창 (스타크래프트 채팅 로그 스타일).
- **API/WS**:
  - `POST /publish/room/{roomId}/chat`: 채팅 및 정답 제출.
  - `CORRECT_ANSWER`: 정답자 하이라이트 및 점수 갱신.
  - `ROUND_TIMEOUT`: 다음 라운드 준비 알림.

#### D. 결과 화면 (Game Result)
- **기능**: 최종 순위표 표시.
- **UI**: 스타크래프트 승리/패배 결과 화면 컨셉.
- **메시지 타입 처리**: `GAME_END`

---

### 4. 백엔드 연동 명세 (API & WebSocket)

#### HTTP REST API
- **방 생성**: `POST /game/room`
  - Request: `{ "title": "방제목", "maxPlayers": 8, "name": "내닉네임" }`
  - Response: `String (roomId)`
- **방 입장**: `POST /game/room/{roomId}/join`
  - Header: `nickname: 내닉네임`
- **게임 시작**: `POST /game/room/{roomId}/start` (방장 전용)
  - Header: `nickname: 내닉네임`

#### WebSocket (STOMP)
- **Endpoint**: `ws-quiz` (SockJS 사용 시 `/ws-quiz`로 접속)
- **구독 경로**: `/subscribe/room/{roomId}`
- **발행 경로**: `/publish/room/{roomId}/chat`
  - Header: `nickname: 내닉네임`
  - Payload: `String (message)`

#### 실시간 메시지 타입 (Message Types)
| Type | Data Fields | Description |
| :--- | :--- | :--- |
| `PLAYER_JOIN` | `nickname` | 플레이어 입장 알림 |
| `PLAYER_LEAVE` | `nickname` | 플레이어 퇴장 알림 |
| `HOST_CHANGE` | `newHost` | 방장 변경 알림 |
| `GAME_START` | `songCount` | 게임 시작 및 총 곡 수 |
| `TIMER_TICK` | `remainingSeconds` | 1초마다 전송되는 남은 시간 (30초/5초) |
| `CORRECT_ANSWER` | `nickname`, `answer`, `score` | 정답자 발생 및 갱신된 점수 |
| `ROUND_TIMEOUT` | `nextSongIndex` | 시간 초과 후 다음 곡 준비 |
| `GAME_END` | `rankings` | 게임 종료 및 최종 점수판 (Map<String, Integer>) |
| `CHAT` | `nickname`, `message` | 일반 플레이어 채팅 메시지 |

---

### 5. 구현 팁 (Implementation Tips)

1. **유튜브 연동**: `react-youtube` 라이브러리를 사용하세요. `GAME_START` 또는 `ROUND_TIMEOUT` 시 전달받는 `songIds` 정보를 통해 서버에서 정의된 퀴즈 곡을 로드해야 합니다. (현재 서버 로직상 `songIds`가 `GAME_START` 시점에 전달되므로, 클라이언트에서 이를 관리해야 합니다.)
2. **세션 관리**: 백엔드에서 `HttpSessionHandshakeInterceptor`를 사용하므로, HTTP 요청 시 설정한 세션(닉네임 등)이 웹소켓 연결 시에도 유지됩니다.
3. **Tailwind 유틸리티**: 스타크래프트 스타일의 `scanline` 애니메이션이나 `terminal` 텍스트 효과를 Tailwind 커스텀 클래스로 추가하여 몰입감을 높이세요.

```css
/* Tailwind 커스텀 예시 */
.sc-panel {
  @apply bg-slate-900 bg-opacity-80 border-2 border-blue-500 rounded-sm p-4;
  box-shadow: inset 0 0 10px #000;
}
```

---
본 가이드를 바탕으로 UI 컴포넌트를 먼저 개발한 뒤, `@stomp/stompjs`를 이용해 서버 이벤트를 연결하는 순서로 진행하는 것을 추천합니다.
