package com.fungame.songquiz.support;

/**
 * {@link SharedStateCleaner} 가 어떤 타입을 치우는지 아키텍처 테스트에 알려준다.
 * 스프링 컨텍스트를 띄우지 않고도 목록을 볼 수 있어야 해서 따로 둔다.
 */
public final class SharedStateCleanerCoverage {

    private SharedStateCleanerCoverage() {
    }

    public static boolean isClassified(String className) {
        return SharedStateCleaner.classifiedTypes().stream()
                .anyMatch(type -> type.getName().equals(className));
    }
}
