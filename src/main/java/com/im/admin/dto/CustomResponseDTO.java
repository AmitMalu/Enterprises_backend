package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class CustomResponseDTO {
    private int statusCode;
    private String message;

}
