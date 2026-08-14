package com.fungame.songquiz.domain.event;

public record PasswordResetRequestedEvent(String email, String rawToken) {
}
