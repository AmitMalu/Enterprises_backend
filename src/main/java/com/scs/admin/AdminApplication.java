package com.scs.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.scs.admin.util.SSLUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


@SpringBootApplication(scanBasePackages = "com.scs.admin")
@EntityScan(basePackages = "com.scs.admin.entities") // Ensure correct package
@EnableCaching
@EnableScheduling
@EnableAsync
public class AdminApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SSLUtil.disableSSL();
        SpringApplication.run(AdminApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // TODO Auto-generated method stub
        return super.configure(builder);
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
