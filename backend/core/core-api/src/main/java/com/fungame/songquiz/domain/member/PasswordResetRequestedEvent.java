package com.fungame.songquiz.domain.member;

public record PasswordResetRequestedEvent(String email, String rawToken) {
}
