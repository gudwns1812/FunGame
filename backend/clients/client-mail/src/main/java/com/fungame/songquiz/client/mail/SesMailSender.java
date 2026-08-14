package com.fungame.songquiz.client.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Slf4j
@Component
@Profile("prod")
public class SesMailSender {

    private static final String CHARSET = "UTF-8";

    private final SesV2Client sesV2Client;
    private final String from;

    public SesMailSender(SesV2Client sesV2Client, @Value("${client.mail.from}") String from) {
        this.sesV2Client = sesV2Client;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        try {
            sesV2Client.sendEmail(sendEmailRequest(to, subject, body));
        } catch (SesV2Exception e) {
            log.error("메일 발송에 실패했습니다. 수신자: {}, 제목: {}", to, subject, e);
        }
    }

    private SendEmailRequest sendEmailRequest(String to, String subject, String body) {
        return SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder().simple(message(subject, body)).build())
                .build();
    }

    private Message message(String subject, String body) {
        return Message.builder()
                .subject(content(subject))
                .body(Body.builder().text(content(body)).build())
                .build();
    }

    private Content content(String data) {
        return Content.builder().data(data).charset(CHARSET).build();
    }
}
