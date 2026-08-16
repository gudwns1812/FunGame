package com.fungame.songquiz.support.error;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CoreException extends RuntimeException {
    private final ErrorType type;

    public CoreException(@NonNull ErrorType type) {
        this(type, type.getMessage());
    }

    public CoreException(@NonNull ErrorType type, @NonNull String message) {
        super(message);
        this.type = type;
    }
}
