export const PLAYER_COLORS = [
    '#FF4444', // 0: 빨
    '#FF8C00', // 1: 주
    '#FFD700', // 2: 노
    '#00CC66', // 3: 초
    '#4488FF', // 4: 파
    '#5544DD', // 5: 남
    '#AA44FF', // 6: 보
    '#AAAAAA', // 7: 검 (검정 배경에서 보이도록 회색)
];

export const PLAYER_COLOR_INDEX_KEY = 'ums_playerColorIndex';

/** 슬롯 번호 → 색상 HEX */
export const getPlayerColor = (index: number | null | undefined): string => {
    if (index === null || index === undefined || index < 0 || index >= PLAYER_COLORS.length) {
        return '#FFFFFF';
    }
    return PLAYER_COLORS[index];
};
