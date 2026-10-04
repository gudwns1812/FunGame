package com.fungame.songquiz.client.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
public class SmtpMailSender {

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender javaMailSender, @Value("${client.mail.from}") String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        try {
            javaMailSender.send(message(to, subject, body));
        } catch (MailException e) {
            log.error("메일 발송에 실패했습니다. 수신자: {}, 제목: {}", to, subject, e);
        }
    }

    private SimpleMailMessage message(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }
}
