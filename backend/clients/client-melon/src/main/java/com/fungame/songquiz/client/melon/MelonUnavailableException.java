package com.fungame.songquiz.client.melon;

public class MelonUnavailableException extends RuntimeException {

    public MelonUnavailableException(String message) {
        super(message);
    }

    public MelonUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
