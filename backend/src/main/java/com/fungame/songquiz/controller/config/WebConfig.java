package com.fungame.songquiz.controller.config;

import com.fungame.songquiz.controller.config.argumentresolver.NickNameDecodeResolver;
import com.fungame.songquiz.controller.config.interceptor.PlayerInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final NickNameDecodeResolver nickNameDecodeResolver;
    private final PlayerInterceptor playerInterceptor;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(nickNameDecodeResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(playerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/game/rooms/*/skip",
                        "/game/player/**",
                        "/game/rooms/*/play/rank",
                        "/game/rooms/*/join",
                        "/ws-stomp/**"
                );
    }
}
