# FunGame (실시간 멀티플레이어 게임 플랫폼)

`FunGame`은 사용자들이 실시간으로 방을 만들고 입장하여 다양한 게임을 즐길 수 있는 멀티플레이어 게임 플랫폼입니다. 실시간 데이터 동기화와 원활한 게임 경험을 위한 안정적인 세션 관리를 목표로 합니다.

---

## 🚀 주요 기능

### 1. 실시간 대기실 및 게임 세션 관리
*   **방 생성 및 입장**: 사용자가 새로운 게임 방을 생성하거나 목록에서 선택하여 입장할 수 있습니다.
*   **준비(Ready) 시스템**: 공정한 게임 시작을 위해 모든 플레이어의 준비 상태를 실시간으로 관리하며, 방장은 모든 인원이 준비된 후 게임을 시작할 수 있습니다.
*   **실시간 동기화**: WebSocket(STOMP)을 사용하여 플레이어 간 상태 변화 및 게임 데이터를 실시간으로 동기화합니다.

### 2. 세션 유지 및 복구 (Re-entry)
*   **네트워크 대응**: 불안정한 네트워크 연결이나 재접속 시에도 기존 점수와 세션을 유지할 수 있도록 설계되었습니다.
*   **유연한 세션 관리**: 짧은 접속 끊김(예: 30초) 동안 플레이어를 퇴장시키지 않고 상태를 유지하여 '이어하기'가 가능합니다.

### 3. 게임 엔진 및 동기화 로직
*   **서버측 타이머 관리**: 서버에서 게임 라운드 및 진행 시간을 정밀하게 기록하여 모든 클라이언트가 동일한 시점에 동기화되도록 합니다.
*   **동적 피드백**: 정답 처리 시 단순 채팅 외에도 시각적 효과를 위한 풍부한 데이터를 전송합니다.

---

## 🛠 기술 스택

### **Backend**
*   **Language**: Java 17+
*   **Framework**: Spring Boot
*   **Build Tool**: Gradle
*   **API Docs**: Spring Rest Docs (MockMvc 기반 자동 생성)
*   **Messaging**: Spring WebSocket (STOMP)

### **Frontend**
*   **Framework**: React (Vite)
*   **Language**: TypeScript
*   **Styling**: Vanilla CSS
*   **Test Tool**: Vitest

---

## 📂 프로젝트 구조

```text
FunGame/
├── backend/        # Spring Boot 기반 백엔드 (Java)
├── frontend/       # React 기반 프론트엔드 (TypeScript)
├── api/            # 자동 생성된 API 명세 (Markdown)
├── docs/           # 프로젝트 설계 및 가이드라인 문서
└── GEMINI.md       # AI 협업 및 작업 규칙
```

---

## 🏃 실행 방법

### **백엔드 실행 (Backend)**
```bash
cd backend
./gradlew bootRun
```

### **프론트엔드 실행 (Frontend)**
```bash
cd frontend
npm install
npm run dev
```

### **로컬 단독 실행 (external DB 없이)**

인메모리 H2 로 백엔드를 띄우고 프론트를 거기에 붙이는 조합입니다.
DB 나 공용 개발 서버 없이 혼자 기능을 확인할 때 쓰세요.

백엔드 (`local` 프로파일: H2 + 시드 데이터 + localhost CORS + Secure 쿠키 해제):
```bash
./gradlew :backend:bootRun --args='--spring.profiles.active=local'
```

프론트엔드 (`localdev` 모드: `http://localhost:8080` 을 바라보고 5199 포트로 기동):
```bash
npm run dev:local --prefix frontend
```

시드는 `backend/src/main/resources/db/local-seed.sql` 에 있습니다.
방 번호 채번용 카운터와 CS 퀴즈 5문제가 들어 있어 CS 퀴즈는 바로 플레이할 수 있습니다.
음악 퀴즈는 유튜브 링크가 필요하므로 관리자 화면에서 곡을 등록한 뒤 사용하세요.

---

## 📜 개발 가이드라인
본 프로젝트는 특정 협업 규칙과 코드 스타일을 준수합니다. 상세 내용은 다음 문서를 참고하세요:
*   [전체 가이드라인 (GEMINI.md)](GEMINI.md)
*   [백엔드 작업 지침 (BACKEND.md)](backend/BACKEND.md)
*   [프론트엔드 작업 지침 (FRONTEND.md)](frontend/docs/FRONTEND.md)
