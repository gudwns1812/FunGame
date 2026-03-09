package com.fungame.songquiz.support.response;

import com.fungame.songquiz.support.error.ErrorMessage;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final ResultType result;
    private final T data;
    private final ErrorMessage error;

    private ApiResponse(ResultType result, T data, ErrorMessage error) {
        this.result = result;
        this.data = data;
        this.error = error;
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null);
    }

    public static ApiResponse<Void> fail(ErrorType error) {
        return new ApiResponse<>(ResultType.FAIL, null, new ErrorMessage(error.getCode(), error.getMessage()));
    }
}
