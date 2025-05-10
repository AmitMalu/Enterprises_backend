package com.im.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.util.Base64;

@SpringBootApplication(scanBasePackages = "com.im.admin")
@EntityScan(basePackages = "com.im.admin.entities") // Ensure correct package
public class AdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminApplication.class, args);
	}

}
