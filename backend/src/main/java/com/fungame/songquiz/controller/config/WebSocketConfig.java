package com.fungame.songquiz.controller.config;

import com.fungame.songquiz.controller.websocket.StompDestination;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-quiz")
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(StompDestination.BROKER_PREFIX);
        registry.setApplicationDestinationPrefixes(StompDestination.APPLICATION_PREFIX);
    }

    /**
     * @MessageMapping 핸들러에서도 REST 컨트롤러와 동일하게 @AuthenticationPrincipal 을 쓰기 위한 등록.
     * 이게 없으면 핸들러가 Principal 을 받아 직접 캐스팅해야 한다.
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    /**
     * 위 리졸버는 메시지의 Principal 헤더가 아니라 SecurityContextHolder 에서 인증을 읽는다.
     * 메시지를 처리하는 스레드의 SecurityContext 를 채워주지 않으면 항상 null 이 주입되므로
     * 이 인터셉터가 반드시 함께 있어야 한다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new SecurityContextChannelInterceptor());
    }
}
