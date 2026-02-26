package com.fungame.songquiz.support.error;

import lombok.Getter;

@Getter
public class CoreException extends RuntimeException {
    private final ErrorType type;

    public CoreException(ErrorType type) {
        super(type.getMessage());
        this.type = type;
    }
}
