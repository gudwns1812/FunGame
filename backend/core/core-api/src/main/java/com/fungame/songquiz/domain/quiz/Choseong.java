package com.fungame.songquiz.domain.quiz;

public final class Choseong {

    private static final char FIRST_SYLLABLE = '가';
    private static final char LAST_SYLLABLE = '힣';
    private static final int SYLLABLES_PER_INITIAL = 21 * 28;
    private static final char[] INITIALS = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private Choseong() {
    }

    public static String of(String text) {
        StringBuilder initials = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                continue;
            }
            initials.append(isSyllable(ch) ? INITIALS[(ch - FIRST_SYLLABLE) / SYLLABLES_PER_INITIAL] : ch);
        }

        return initials.toString();
    }

    public static boolean isHangulTitle(String title) {
        boolean hasSyllable = false;
        for (char ch : title.toCharArray()) {
            if (isSyllable(ch)) {
                hasSyllable = true;
            } else if (Character.isLetter(ch)) {
                return false;
            }
        }

        return hasSyllable;
    }

    private static boolean isSyllable(char ch) {
        return ch >= FIRST_SYLLABLE && ch <= LAST_SYLLABLE;
    }
}
