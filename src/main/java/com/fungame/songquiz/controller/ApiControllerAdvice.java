package com.fungame.songquiz.controller;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ApiControllerAdvice {

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoreException(CoreException e) {
        switch (e.getType().getLogLevel()) {
            case DEBUG -> log.debug(e.getMessage());
            case WARN -> log.warn(e.getMessage());
            case ERROR -> log.error(e.getMessage());
        }

        return new ResponseEntity<>(ApiResponse.fail(e.getType()), e.getType().getStatus());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleDefaultException(Exception e) {
        log.error("Exception : ", e);
        return ApiResponse.fail(ErrorType.DEFAULT_ERROR);
    }
}
