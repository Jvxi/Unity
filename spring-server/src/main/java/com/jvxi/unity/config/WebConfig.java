package com.jvxi.unity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cat-tool.upload-dir:uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Paths.get(uploadRoot).toAbsolutePath().normalize().toUri().toString();
        if (!uploadDir.endsWith("/")) {
            uploadDir += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
    }
}
