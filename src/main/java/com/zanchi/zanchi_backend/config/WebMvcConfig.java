package com.zanchi.zanchi_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.root}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = uploadRoot.replace("\\", "/");
        if (!root.endsWith("/")) root += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root);
    }
}
