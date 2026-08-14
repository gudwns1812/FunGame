package com.fungame.songquiz.support.mail;

import com.fungame.songquiz.domain.member.PasswordResetTokenGenerator;
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
public class SesPasswordResetMailSender implements PasswordResetMailSender {

    private static final String SUBJECT = "[FunGame] 비밀번호 재설정 안내";
    private static final String CHARSET = "UTF-8";

    private final SesV2Client sesV2Client;
    private final String from;

    public SesPasswordResetMailSender(SesV2Client sesV2Client, @Value("${app.mail.from}") String from) {
        this.sesV2Client = sesV2Client;
        this.from = from;
    }

    @Override
    public void send(String email, String resetLink) {
        try {
            sesV2Client.sendEmail(sendEmailRequest(email, resetLink));
        } catch (SesV2Exception e) {
            log.error("비밀번호 재설정 메일 발송에 실패했습니다. 수신자: {}", email, e);
        }
    }

    private SendEmailRequest sendEmailRequest(String email, String resetLink) {
        return SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(email).build())
                .content(EmailContent.builder().simple(message(resetLink)).build())
                .build();
    }

    private Message message(String resetLink) {
        return Message.builder()
                .subject(content(SUBJECT))
                .body(Body.builder().text(content(body(resetLink))).build())
                .build();
    }

    private Content content(String data) {
        return Content.builder().data(data).charset(CHARSET).build();
    }

    private String body(String resetLink) {
        return """
                아래 링크에서 새 비밀번호를 설정해주세요.

                %s

                링크는 %d분 뒤에 만료되고, 한 번만 사용할 수 있습니다.
                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(resetLink, PasswordResetTokenGenerator.TOKEN_TTL.toMinutes());
    }
}
