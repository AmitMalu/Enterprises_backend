package com.scs.admin.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenGenerationRequest {

    private String username;
    private String password;
    private String salt;
    private String ivBase64;
}
