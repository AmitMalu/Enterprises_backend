package com.scs.admin.response;


import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommonResponse<T> {

    private int statusCode;
    private String message;
    private T data;

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> CommonResponse<T> failure(String message) {
        return new CommonResponse<>(HttpStatus.NOT_FOUND.value(), message, null);
    }

    public static <T> CommonResponse<T> created(T data, String message) {
        return new CommonResponse<>(HttpStatus.CREATED.value(), message, data);
    }

    public static <T> CommonResponse<T> error(int code, String message) {
        return new CommonResponse<>(code, message, null);
    }

    public static <T> CommonResponse<T> handleErrors(String message) {
        return new CommonResponse<>(HttpStatus.BAD_REQUEST.value(), message, null);
    }

    public static <T> CommonResponse<T> failedResponse(int code, String message, T data) {
        return new CommonResponse<>(code, message, data);
    }
}
