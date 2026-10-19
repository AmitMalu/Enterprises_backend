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
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Set;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    /** API server shown in Swagger "Try it out"; set per environment (see application-*.properties). */
    @Value("${springdoc.servers[0].url:https://api.nfttech.in/enterprises}")
    private String swaggerServerUrl;

    @Value("${springdoc.servers[0].description:Production Server (HTTPS)}")
    private String swaggerServerDescription;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/custom.js")
                .addResourceLocations("classpath:/static/swagger-custom.js");
    }

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                // ===== ADD SERVERS HERE =====
                .servers(List.of(
                        new Server()
                                .url(swaggerServerUrl)
                                .description(swaggerServerDescription)
                ))
                // ===== END OF SERVERS CONFIGURATION =====
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
                        .title("Smart Credit Solution API")
                        .version("1.0")
                        .description("Smart Credit Solution platform API documentation")
                        .contact(new Contact()
                                .name("Smart Credit Solution Team")));
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



