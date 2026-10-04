package com.fungame.songquiz.controller.websocket;

import org.springframework.util.StringUtils;

import java.util.Map;

public record BroadcastMessage(String instanceId, String destination, String user, String payload) {

    public static final String EVERYONE = "";

    private static final String INSTANCE_FIELD = "instance";
    private static final String DESTINATION_FIELD = "destination";
    private static final String USER_FIELD = "user";
    private static final String PAYLOAD_FIELD = "payload";

    public static BroadcastMessage from(Map<String, String> fields) {
        return new BroadcastMessage(
                fields.get(INSTANCE_FIELD),
                fields.get(DESTINATION_FIELD),
                fields.get(USER_FIELD),
                fields.get(PAYLOAD_FIELD));
    }

    public Map<String, String> toFields() {
        return Map.of(
                INSTANCE_FIELD, instanceId,
                DESTINATION_FIELD, destination,
                USER_FIELD, user,
                PAYLOAD_FIELD, payload);
    }

    public boolean isForEveryone() {
        return !StringUtils.hasText(user);
    }
}
