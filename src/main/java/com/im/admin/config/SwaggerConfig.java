package com.im.admin.config;

/**
 * @author SHUBHAM KHOPADE
 */

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/custom.js")
                .addResourceLocations("classpath:/static/swagger-custom.js");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Admin Portal API")
                        .version("1.0")
                        .description("Enterprise Admin Portal API Documentation")
                        .contact(new Contact()
                                .name("Admin Team")
                                .email("support@enterprise.com")));
    }
}
