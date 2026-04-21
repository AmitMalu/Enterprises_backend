package com.im.admin.request;

import lombok.Data;

@Data
public class SmsCredentialsRequestDto {

    private String baseUrl;
    private String username;
    private String password;
    private String adminId;
    private String senderId;
    private String peId;
    private String dltTemplateId;
    private String route;
    private String whitelabelId;
    private String type;
}
