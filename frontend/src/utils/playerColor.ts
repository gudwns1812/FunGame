export const PLAYER_COLORS = [
    '#FF4444', // 0: 빨
    '#FF8C00', // 1: 주
    '#FFD700', // 2: 노
    '#00CC66', // 3: 초
    '#4488FF', // 4: 파
    '#5544DD', // 5: 남
    '#AA44FF', // 6: 보
    '#AAAAAA', // 7: 검 (검정 배경에서 보이도록 회색)
    '#FF69B4', // 8: 분홍
    '#00FFFF', // 9: 청록
    '#32CD32', // 10: 라임 초록
    '#DDA0DD', // 11: 연보라
];

export const PLAYER_COLOR_INDEX_KEY = 'ums_playerColorIndex';

/** 슬롯 번호 → 색상 HEX */
export const getPlayerColor = (index: number | null | undefined): string | null => {
    if (index === null || index === undefined || index < 0 || index >= PLAYER_COLORS.length) {
        return null;
    }
    return PLAYER_COLORS[index];
};
