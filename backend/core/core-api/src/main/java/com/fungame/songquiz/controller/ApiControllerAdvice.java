package com.fungame.songquiz.controller;

import com.fungame.songquiz.controller.response.ApiResponse;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ApiControllerAdvice {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoreException(CoreException e) {
        ErrorType type = e.getType();
        switch (type.getLogLevel()) {
            case DEBUG -> log.debug("{} : {}", type.getCode(), e.getMessage());
            case WARN -> log.warn("{} : {}", type.getCode(), e.getMessage());
            case ERROR -> log.error("{} : {}", type.getCode(), e.getMessage(), e);
        }

        return new ResponseEntity<>(ApiResponse.fail(e), type.getStatus());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.debug("로그인 실패 : {}", e.getMessage());
        return new ResponseEntity<>(ApiResponse.fail(ErrorType.LOGIN_FAILED), ErrorType.LOGIN_FAILED.getStatus());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        log.warn("BindException : {}", e.getMessage());
        return ApiResponse.fail(ErrorType.INVALID_INPUT_VALUE);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleDefaultException(Exception e) {
        log.error("Exception : ", e);
        return ApiResponse.fail(ErrorType.DEFAULT_ERROR);
    }
}
