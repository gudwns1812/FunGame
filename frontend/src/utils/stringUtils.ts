/**
 * 서버에서 닉네임에 붙여주는 태그(#001 등)를 제거하고 순수 닉네임만 반환합니다.
 * @example stripTag('홍길동#001') // => '홍길동'
 * @example stripTag('홍길동') // => '홍길동'
 */
export const stripTag = (nickname: string): string => {
    const idx = nickname.indexOf('#');
    return idx !== -1 ? nickname.slice(0, idx) : nickname;
};
