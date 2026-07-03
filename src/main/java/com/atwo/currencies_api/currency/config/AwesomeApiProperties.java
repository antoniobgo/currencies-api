package com.atwo.currencies_api.currency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "awesome-api")
public record AwesomeApiProperties(String baseUrl, String apiKey) {}
