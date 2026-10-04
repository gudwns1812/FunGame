package com.fungame.songquiz.support.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class InstanceId {

    private static final int GENERATED_LENGTH = 8;

    private final String value;

    public InstanceId(@Value("${app.instance-id:}") String configured) {
        this.value = StringUtils.hasText(configured)
                ? configured
                : UUID.randomUUID().toString().substring(0, GENERATED_LENGTH);
    }

    public String value() {
        return value;
    }

    public boolean isMine(String candidate) {
        return value.equals(candidate);
    }
}
