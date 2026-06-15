package com.im.admin.controller;

/**
 * @author SHUBHAM KHOPADE
 */
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestingMethods {

    public static void main(String[] args) {

        String password = "Test@111";

        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String encodedPassword = encoder.encode(password);

        System.out.println("Encoded Password : " + encodedPassword);

        System.out.println(
                "Matches : " +
                        encoder.matches(password, encodedPassword)
        );
    }
}
