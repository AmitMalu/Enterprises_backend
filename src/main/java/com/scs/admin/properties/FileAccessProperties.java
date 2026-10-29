package com.scs.admin.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file.access")
@Getter
@Setter
public class FileAccessProperties {

    private String baseUrl;

}
