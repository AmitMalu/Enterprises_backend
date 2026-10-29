package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EncryptDecryptCredentialsDto {

    private String username;
    private String password;
    private String salt;
    private String iv;

    public EncryptDecryptCredentialsDto(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
