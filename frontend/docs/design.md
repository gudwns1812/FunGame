# 프론트엔드 디자인 가이드라인 (design.md)

본 문서는 `stitch.zip`의 디자인 자산을 분석하여 도출한 **'Cosmic & Sci-Fi'** 테마 가이드라인입니다. 스타크래프트 UMS나 우주선 관제 시스템의 감성을 기반으로 각 페이지와 컴포넌트를 설계합니다.

---

## 1. 디자인 컨셉: Cosmic Control Center

- **전체 테마**: 다크 모드 기반의 미래지향적 인터페이스 (Sci-Fi UI).
- **핵심 키워드**: 네온 블루, 격자 배경, 패널 레이아웃, 기하학적 폰트.

---

## 2. 시각적 요소 (Visual Identity)

### A. 컬러 팔레트 (Color Palette)
- **Primary (Highlight)**: `#25c0f4` (Cyan Blue)
    - 텍스트 강조, 버튼 배경, 네온 효과에 사용.
- **Main Background**: `#101e22` (Deep Space Teal)
    - 전체 화면의 기본 배경색.
- **Panel Background**: `rgba(16, 30, 34, 0.9)` (Translucent Dark)
    - 콘텐츠 영역(카드, 모달) 배경.
- **Secondary / Borders**: `rgba(37, 192, 244, 0.3)`
    - 경계선 및 격자 무늬에 사용.

### B. 타이포그래피 (Typography)
- **글꼴**: `Space Grotesk` (Google Fonts)
- **스타일**:
    - 제목: 대문자(Uppercase), 볼드, `tracking-widest` (자간 넓게).
    - 본문: 가독성 중심의 중간 굵기.
    - 상태창: Monospace 느낌의 폰트 활용.

### C. 주요 효과
- **Neon Glow**: `text-shadow: 0 0 10px rgba(37, 192, 244, 0.8)`
- **Grid Overlay**: 배경에 40px 간격의 옅은 블루 격자 무늬 고정.
- **Panel Border**: 1px의 Primary 투명도 경계선과 내부 그림자(`inset`)를 조합하여 입체감 부여.

---

## 3. 페이지별 디자인 적용 가이드

### A. 닉네임 페이지 (LOBBY)
- **배경**: 전체 화면 격자 무늬 및 중앙에 'PILOT ENROLLMENT' 패널 배치.
- **입력창**: 배경은 더 어두운 블랙, 경계선은 Primary 네온 효과 적용.
- **버튼**: 'LAUNCH MISSION' 텍스트와 함께 Primary 배경색의 볼드한 버튼. 클릭 시 살짝 커지는 애니메이션.

### B. 방 목록 페이지 (ROOM_LIST)
- **헤더**: 상단에 시스템 상태(`SYSTEM STATUS: OPTIMAL`)와 현재 구역(`SECTOR: KOPRULU-IV`) 정보를 표시하는 관제 바 배치.
- **방 카드**: 얇은 시안색 테두리의 패널 형태. 호스트 이름 옆에 작은 아이콘(아이콘: `shield_person`) 배치.
- **스크롤바**: 매우 얇은 시안색의 커스텀 스크롤바 적용.

### C. 게임 대기실 (WAITING)
- **플레이어 슬롯**: 8개의 슬롯을 격자 형태로 배치. 빈 슬롯은 점선 테두리와 `EMPTY` 텍스트로 처리.
- **준비 상태**: 준비 완료 시 닉네임 옆에 밝은 시안색 체크 표시와 글로우 효과.
- **채팅창**: 왼쪽 하단에 고정된 투명한 패널. 텍스트는 밝은 녹색이나 시안색으로 처리하여 터미널 느낌 강조.

### D. 인게임 (PLAYING)
- **상단 타이머**: 화면 중앙 상단에 큰 디지털 숫자와 함께 게이지 바(`Stability Bar`) 형태의 진행률 표시.
- **유튜브 영역**: 중앙에 배치하되, 패널 테두리를 둘러 우주선 내부 모니터를 보는 듯한 프레임 적용.
- **점수판**: 우측 상단에 실시간으로 순위가 바뀌는 미니 랭킹 패널.

### E. 결과 화면 (RESULT)
- **최종 순위**: 'MISSION COMPLETE' 문구와 함께 1, 2, 3위 플레이어를 강조하는 큰 네온 패널.
- **통계 데이터**: 폰트 크기를 줄이고 자간을 넓힌 Monospace 텍스트로 상세 기록 출력.

---

## 4. 컴포넌트 라이브러리 (기능적 디자인)

- **Button**: 기본적으로 사각형이며, 모서리에 아주 작은 Round(`rounded-sm`) 적용. 호버 시 `bg-primary/10` 또는 전체 채우기 전환.
- **Input**: `outline-none` 처리하고 `focus` 시 경고등이 들어오듯 테두리가 밝아지는 효과.
- **Progress Bar**: `bg-slate-800` 바탕에 Primary 색상이 채워지는 형태. 끝부분에 강한 빛 효과(`shadow-glow`) 추가.

---
*본 디자인 가이드는 프론트엔드 리팩토링 시 UI의 일관성을 유지하는 기준이 됩니다.*
