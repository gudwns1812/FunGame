package com.fungame.songquiz.support.mail;

public interface PasswordResetMailSender {

    void send(String email, String resetLink);
}
