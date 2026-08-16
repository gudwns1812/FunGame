package com.fungame.songquiz.domain.member;

public final class PasswordPolicy {

    public static final int MINIMUM_LENGTH = 4;

    private PasswordPolicy() {
    }

    public static boolean isSatisfiedBy(String password) {
        return password != null && password.length() >= MINIMUM_LENGTH;
    }

    public static String violationMessage() {
        return "비밀번호는 " + MINIMUM_LENGTH + "자 이상이어야 합니다.";
    }
}
