package com.fungame.songquiz.client.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@Profile("prod")
public class SesClientConfig {

    @Bean
    public SesV2Client sesV2Client(@Value("${client.mail.aws-region}") String region) {
        return SesV2Client.builder()
                .region(Region.of(region))
                .build();
    }
}
