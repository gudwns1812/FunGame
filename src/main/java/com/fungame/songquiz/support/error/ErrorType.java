package com.fungame.songquiz.support.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),

    GAME_ROOM_NOT_FOUND(HttpStatus.BAD_REQUEST, ErrorCode.G002, "해당 id의 방을 찾을 수 없습니다.", LogLevel.ERROR),
    GAME_ROOM_MAX_PLAYER_EXCEED(HttpStatus.BAD_REQUEST, ErrorCode.G001, "방이 가득찼습니다.", LogLevel.DEBUG),
    GAME_ROOM_PLAYER_EMPTY(HttpStatus.BAD_REQUEST, ErrorCode.G003, "방에 플레이어가 비어있습니다.", LogLevel.ERROR),
    GAME_ROOM_NOT_HOST(HttpStatus.FORBIDDEN, ErrorCode.G004, "방장만 게임을 시작할 수 있습니다.", LogLevel.DEBUG),
    GAME_ROOM_ALREADY_PLAYING(HttpStatus.BAD_REQUEST, ErrorCode.G006, "이미 진행 중인 게임입니다.", LogLevel.DEBUG),
    GAME_ROOM_LOCK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.G005, "잠시 후 다시 시도해주세요.", LogLevel.ERROR);

    private final HttpStatus status;
    private final ErrorCode code;
    private final String message;
    private final LogLevel logLevel;

    ErrorType(HttpStatus status, ErrorCode code, String message, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }
}
