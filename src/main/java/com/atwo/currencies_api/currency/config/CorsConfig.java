package com.atwo.currencies_api.currency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173",
                                "https://currencies-front.antonio-sf-dev.workers.dev",
                                "https://diariodeumdev.com", "https://www.diariodeumdev.com",
                                "https://cotacoins.online", "https://www.cotacoins.online")
                        .allowedMethods("GET", "POST", "DELETE").allowedHeaders("*").maxAge(3600);
            }
        };
    }
}
