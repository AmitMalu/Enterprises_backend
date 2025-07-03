package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
}
