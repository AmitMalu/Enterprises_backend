package com.im.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        // Create custom Jackson converter with our configured ObjectMapper
        MappingJackson2HttpMessageConverter customJacksonConverter = new MappingJackson2HttpMessageConverter();
        customJacksonConverter.setObjectMapper(objectMapper);

        // Build RestTemplate with default converters
        RestTemplate restTemplate = builder.build();

        // Get current converters and add our custom one at the beginning
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());

        // Remove any existing MappingJackson2HttpMessageConverter to avoid conflicts
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);

        // Add our custom converter at the beginning
        converters.add(0, customJacksonConverter);

        restTemplate.setMessageConverters(converters);
        return restTemplate;
    }
}

