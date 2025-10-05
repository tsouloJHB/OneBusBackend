package com.backend.onebus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.dir:media}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/" + uploadDir + "/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(3600); // Cache for 1 hour
        
        // Serve static resources from classpath (optional)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}