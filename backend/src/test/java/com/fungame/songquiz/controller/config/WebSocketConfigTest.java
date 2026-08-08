package com.fungame.songquiz.controller.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    private SimpAnnotationMethodMessageHandler messageHandler;

    @Autowired
    @Qualifier("clientInboundChannel")
    private AbstractSubscribableChannel clientInboundChannel;

    @Autowired
    private SimpleBrokerMessageHandler brokerMessageHandler;

    @Test
    @DisplayName("@MessageMapping 핸들러가 @AuthenticationPrincipal 을 주입받으려면 리졸버가 등록돼 있어야 한다.")
    void authenticationPrincipalResolverIsRegistered() {
        assertThat(messageHandler.getCustomArgumentResolvers())
                .hasAtLeastOneElementOfType(AuthenticationPrincipalArgumentResolver.class);
    }

    @Test
    @DisplayName("비정상 종료를 감지하려면 브로커에 하트비트가 설정돼 있어야 한다.")
    void heartbeatIsEnabled() {
        assertThat(brokerMessageHandler.getHeartbeatValue())
                .as("하트비트가 없으면 TCP 가 half-open 으로 남는 끊김을 감지하지 못한다")
                .isNotNull()
                .containsExactly(10_000L, 10_000L);
        assertThat(brokerMessageHandler.getTaskScheduler())
                .as("하트비트에는 TaskScheduler 가 필수다")
                .isNotNull();
    }

    @Test
    @DisplayName("리졸버는 SecurityContextHolder 에서 인증을 읽으므로 인바운드 채널에 SecurityContext 인터셉터가 필요하다.")
    void securityContextIsPropagatedToMessageHandlingThread() {
        assertThat(clientInboundChannel.getInterceptors())
                .hasAtLeastOneElementOfType(SecurityContextChannelInterceptor.class);
    }
}
