package com.fungame.songquiz.support.error;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            LogLevel.ERROR),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, ErrorCode.C001, "잘못된 입력값입니다.", LogLevel.WARN),

    GAME_ROOM_NOT_FOUND(HttpStatus.BAD_REQUEST, ErrorCode.G002, "해당 id의 방을 찾을 수 없습니다.", LogLevel.ERROR),
    GAME_ROOM_MAX_PLAYER_EXCEED(HttpStatus.BAD_REQUEST, ErrorCode.G001, "방이 가득찼습니다.", LogLevel.DEBUG),
    GAME_ROOM_PLAYER_EMPTY(HttpStatus.BAD_REQUEST, ErrorCode.G003, "방에 플레이어가 비어있습니다.", LogLevel.ERROR),
    GAME_ROOM_NOT_HOST(HttpStatus.FORBIDDEN, ErrorCode.G004, "방장만 게임을 시작할 수 있습니다.", LogLevel.DEBUG),
    GAME_ALREADY_PLAYING(HttpStatus.BAD_REQUEST, ErrorCode.G006, "이미 진행 중인 게임입니다.", LogLevel.DEBUG),
    GAME_ROOM_LOCK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.G005, "잠시 후 다시 시도해주세요.", LogLevel.ERROR),
    GAME_ROOM_PLAYER_DUPLICATE(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.P001, "플레이어가 중복입니다.", LogLevel.ERROR),

    NOT_VALID_HOST(HttpStatus.BAD_REQUEST, ErrorCode.G007, "호스트가 아닙니다.", LogLevel.DEBUG),

    GAME_NOT_FOUND(HttpStatus.BAD_REQUEST, ErrorCode.G008 , "게임이 없습니다.", LogLevel.WARN),
    GAME_ROOM_NOT_ALL_READY(HttpStatus.BAD_REQUEST, ErrorCode.G009, "모든 플레이어가 준비되지 않았습니다.", LogLevel.DEBUG),

    PLAYER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.P002, "플레이어를 찾을 수 없습니다.", LogLevel.ERROR),
    QUIZ_DUPLICATE_ERROR(HttpStatus.BAD_REQUEST,ErrorCode.G010 , "노래가 중복입니다.",LogLevel.DEBUG ),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.M001, "해당 사용자를 찾을 수 없습니다.", LogLevel.WARN),
    LOGIN_ID_DUPLICATED(HttpStatus.BAD_REQUEST, ErrorCode.M002, "이미 사용 중인 아이디입니다.", LogLevel.DEBUG),
    NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST, ErrorCode.M003, "이미 사용 중인 닉네임입니다.", LogLevel.DEBUG),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, ErrorCode.M004, "비밀번호가 일치하지 않습니다.", LogLevel.DEBUG),
    PROMOTION_ALREADY_PENDING(HttpStatus.BAD_REQUEST, ErrorCode.M005, "이미 진행 중인 승급 요청이 있습니다.", LogLevel.DEBUG),
    PROMOTION_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorCode.M006, "승급 요청을 찾을 수 없습니다.", LogLevel.WARN);

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
