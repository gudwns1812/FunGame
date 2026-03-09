# 닉네임 태그 숨기기 작업 계획

## 배경 / 문제
- 서버에서 닉네임 중복 방지 및 사용자 식별을 위해 `닉네임#001`과 같이 태그를 붙여서 데이터를 내려주기로 결정됨.
- 하지만 화면(UI) 상에는 태그가 붙은 지저분한 닉네임 대신, 태그를 제외한 순수 닉네임만 표시되어야 함.
- 서버와의 통신 및 내부 상태 값별 `id`, `name` 등에는 태그가 포함된 전체 식별자를 그대로 사용하고, 렌더링 시점에만 태그를 잘라내는(strip) 처리가 필요함.

## 목표
- 화면에 표시되는 사용자 이름에서 `#태그` 부분을 제거하여 순수 닉네임만 렌더링하도록 변경.
- 게임 로직 및 API 통신에는 기존과 동일하게 태그가 포함된 전체 닉네임을 사용.

## 변경 계획

### 1. 닉네임 포맷팅 유틸리티 함수 추가
- 새로운 파일 `src/utils/stringUtils.ts` (또는 기존 유틸 파일) 생성.
- `stripTag(nickname: string): string` 함수 구현.
  - `#` 문자를 기준으로 문자열을 분리하고, 첫 번째 부분만 반환. (예: `홍길동#123` -> `홍길동`)
  - `#`이 없는 경우 그대로 반환.

### 2. UI 컴포넌트 수정 (태그 제거 적용)
화면에 닉네임이 렌더링되는 모든 곳에 `stripTag` 유틸리티를 적용합니다.

- **`src/components/RoomList.tsx`**
  - 상단 환영 메시지: `환영합니다, {nickname}님` -> `환영합니다, {stripTag(nickname)}님`
  - 방 목록에서 방장의 이름(표시되는 경우)
- **`src/components/WaitingRoom.tsx`**
  - 대기실의 참가자 목록 렌더링 부분 (`{player.name}` -> `{stripTag(player.name)}`)
- **`src/components/Game.tsx`**
  - 게임 중 플레이어 점수 및 현재 차례 플레이어 이름 표시 부분
- **`src/components/Result.tsx`**
  - 게임 종료 후 결과(순위) 화면의 플레이어 이름 표시 부분

### 3. 알림 및 채팅 메시지 로그 수정
- **`src/hooks/useGameLogic.ts`**
  - 시스템 로그 및 채팅 메시지를 상태(logs)에 추가할 때 태그 제거 적용.
  - 채팅: `addLog(\`\${stripTag(event.nickname)}: \${event.message}\`)`
  - 정답 안내: `addLog(\`[시스템] \${stripTag(event.nickname)}님이 정답을 맞혔습니다! ...\`)`
  - 호스트 변경 알림 등 닉네임이 포함된 기타 로그 문자열 생성 부분.

## 테스트 시나리오
1. **로그인 및 방 참가**: 태그가 포함된 닉네임을 가진 사용자가 방에 입장했을 때, 대기실/게임방 UI에 태그 없는 이름만 표시되는지 확인.
2. **채팅**: 채팅을 보냈을 때 채팅 기록에 보낸 사람의 닉네임이 태그 없이 표시되는지 확인.
3. **게임 플레이**: 정답을 맞혔을 때 시스템 메시지 및 점수판에서 태그 없는 이름으로 업데이트되는지 확인.
4. **통신 확인**: 브라우저 네트워크 탭 및 웹소켓 메시지 등을 통해 실제 서버로 전송되는 데이터에는 태그가 포함된 전체 닉네임이 잘 전달되는지 확인.

## 작업 범위(수정/추가 파일)
- `src/utils/stringUtils.ts` (신규 또는 수정)
- `src/components/RoomList.tsx`
- `src/components/WaitingRoom.tsx`
- `src/components/Game.tsx`
- `src/components/Result.tsx`
- `src/hooks/useGameLogic.ts`
