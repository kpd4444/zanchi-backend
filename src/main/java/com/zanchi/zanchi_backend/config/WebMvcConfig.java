package com.zanchi.zanchi_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로 허용
                .allowedOrigins("http://127.0.0.1:5500") // HTML 테스트 페이지 주소
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Authorization") // 프론트에서 Authorization 읽을 수 있게
                .allowCredentials(true);
    }
}
