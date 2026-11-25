package com.instakill.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    // citeste media.base-path din application.yaml (daca lipseste, foloseste "media")
    @Value("${media.base-path:media}")
    private String basePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // cererile care incep cu /media/ sunt servite direct din folderul de pe disc
        // atentie: "file:" la inceput si "/" la final sunt obligatorii
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + basePath + "/");
    }
}