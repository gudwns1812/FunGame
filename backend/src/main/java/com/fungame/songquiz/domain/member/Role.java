package com.fungame.songquiz.domain.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    MASTER("ROLE_MASTER", "전체 관리자"),
    ADMIN("ROLE_ADMIN", "시스템 관리자"),
    USER("ROLE_USER", "일반 사용자");

    private final String key;
    private final String title;
}
