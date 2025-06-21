package com.im.admin.commons;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Test {

    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("king@1234");

        System.out.println("Encrypted Password: " + encoded);
    }
}

