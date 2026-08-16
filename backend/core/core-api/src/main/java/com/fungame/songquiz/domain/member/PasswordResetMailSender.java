package com.fungame.songquiz.domain.member;

public interface PasswordResetMailSender {

    void send(String email, String resetLink);
}
