package com.im.admin.request;

import lombok.Data;

@Data
public class EmailCredentialsRequestDto {

    private String email;
    private String password;
    private String type;
    private String adminId;
    private String whitelabelId;
    private String smtpHost;
    private Integer smtpPort;
}
