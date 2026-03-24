# 작업 계획: 플레이어 색상 확장 (최대 12명)

## 1. 개요
현재 `frontend/src/utils/playerColor.ts` 파일의 `PLAYER_COLORS` 배열에는 8명 분의 색상만 정의되어 있습니다. 최대 참가 인원이 12명이므로 4가지의 추가 색상을 정의하여 확장합니다. 

## 2. 변경 내용 및 대상 파일
- `frontend/src/utils/playerColor.ts`

### 3. 상세 수정 내용
1. `PLAYER_COLORS` 배열에 4가지 새로운 색상(HEX)을 추가합니다.
   - 기존(8명): 빨, 주, 노, 초, 파, 남, 보, 검(회색 배경 대응)
   - 추가(4명): 핑크(Pink), 청록(Cyan), 옐로우그린(Yellow Green), 브라운(Brown) 등 뚜렷하게 구별 가능한 색상을 추가합니다.
2. 기존 로직인 `getPlayerColor` 함수는 배열의 길이(`PLAYER_COLORS.length`)에 의존하므로, 추가적인 로직 변경 없이 배열 요소 확장만으로 문제 없이 12명까지 지원이 가능합니다.

이 계획으로 변경 작업을 진행해도 될지 확인 부탁드립니다.
