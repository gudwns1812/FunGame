package com.fungame.songquiz.controller.websocket;

import org.springframework.util.StringUtils;

public record BroadcastMessage(String instanceId, String destination, String user, Object payload) {

    public static final String EVERYONE = "";

    public boolean isForEveryone() {
        return !StringUtils.hasText(user);
    }
}
