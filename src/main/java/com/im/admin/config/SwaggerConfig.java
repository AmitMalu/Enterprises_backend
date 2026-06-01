package com.im.admin.config;

/**
 * @author SHUBHAM KHOPADE
 */

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Set;
import org.springdoc.core.models.GroupedOpenApi;
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
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .tags(List.of(
                        new Tag()
                                .name("Authentication & AES")
                                .description("Authentication APIs")
                ))
                .info(new Info()
                        .title("Enterprise Admin Portal API")
                        .version("1.0")
                        .description("Enterprise Admin Portal API Documentation")
                        .contact(new Contact()
                                .name("Admin Team")
                                .email("support@enterprise.com")));
    }

    @Bean
    public GroupedOpenApi clientApi() {
        Set<String> allowedControllers = Set.of(
            "dmtremittercontroller", "payoutbeneficiarycontroller", "dmtbeneficiarycontroller",
            "aepsinstantpaycontroller", "billpayconfigcontroller", "billavenueencryptcontroller",
            "wallettowallettransfercontroller", "payoutcontroller", "rechargecontroller",
            "rechargeoperatorcontroller", "paymentgatewaycontroller", "prefundrequestcontroller",
            "movetobankcontroller", "aepsmerchantcontroller", "instantpayaepstransactioncontroller",
            "fundtransfercontroller", "dmtcontroller", "billavenuecontroller",
            "bbpscontroller", "bbpsproviderlistcontroller", "aepsbalanceinquirycontroller",
            "aepscashwithdrawalcontroller", "aepsekyccontroller", "aepstwofactorcontroller",
            "aescontroller" // Needed for generate-token
        );

        return GroupedOpenApi.builder()
                .group("client-api")
                .addOpenApiMethodFilter(method -> {
                    String className = method.getDeclaringClass().getSimpleName().toLowerCase();
                    return allowedControllers.contains(className);
                })
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .pathsToMatch("/**")
                .build();
    }
}



