package com.fungame.songquiz.support.error;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CoreException extends RuntimeException {
    private final ErrorType type;

    public CoreException(@NonNull ErrorType type) {
        super(type.getMessage());
        this.type = type;
    }
}
