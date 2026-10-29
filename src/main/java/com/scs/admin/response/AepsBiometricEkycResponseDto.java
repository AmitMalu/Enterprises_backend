package com.scs.admin.response;

import lombok.Data;

@Data
public class AepsBiometricEkycResponseDto {
    private boolean status;
    private String message;
    private Object data;   // Always null as per documentation
    private long statusCode;
}
